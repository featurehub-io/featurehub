package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironmentFeatureStrategy;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.FeatureState;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbApplication;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.publish.CacheSource;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.ApplicationGroupRole;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.EnvironmentGroupRole;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ApplicationSqlApi implements ApplicationApi {
  private static final Logger log = LoggerFactory.getLogger(ApplicationSqlApi.class);
  private final Database database;
  private final Conversions convertUtils;
  private final CacheSource cacheSource;
  private final ArchiveStrategy archiveStrategy;

  @Inject
  public ApplicationSqlApi(Database database, Conversions convertUtils, CacheSource cacheSource, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public Application createApplication(String portfolioId, Application application, Person current) throws DuplicateApplicationException {
    DbPortfolio portfolio = convertUtils.uuidPortfolio(portfolioId);
    if (portfolio != null) {
      DbPerson updater = convertUtils.uuidPerson(current);

      if (updater != null) {
        if (new QDbApplication().and().name.iequalTo(application.getName()).whenArchived.isNull().portfolio.eq(portfolio).endAnd().findCount() == 0) {
          DbApplication aApp = new DbApplication.Builder()
            .name(application.getName())
            .description(application.getDescription())
            .portfolio(portfolio)
            .whoCreated(updater)
            .build();

          // update the portfolio group to ensure it has permissions to add features to this new application
          final DbGroup adminGroup = new QDbGroup().owningPortfolio.eq(aApp.getPortfolio()).whenArchived.isNull().adminGroup.isTrue().findOne();
          if (adminGroup != null) {
            adminGroup.getGroupRolesAcl().add(new DbAcl.Builder().application(aApp).roles(GroupSqlApi.appRolesToString(Collections.singletonList(ApplicationRoleType.FEATURE_EDIT))).build());
          }
          addApplicationFeatureCreationRoleToPortfolioAdminGroup(aApp, adminGroup);

          return convertUtils.toApplication(aApp, Opts.empty());
        } else {
          throw new DuplicateApplicationException();
        }
      }
    }

    return null;
  }

  @Transactional
  private void addApplicationFeatureCreationRoleToPortfolioAdminGroup(DbApplication aApp, DbGroup adminGroup) {
    database.save(aApp);

    if (adminGroup != null) {
      database.save(adminGroup);
    }
  }

  @Transactional
  private void saveApp(DbApplication app) {
    database.save(app);
  }

  @Override
  public List<Application> findApplications(String portfolioId, String filter, SortOrder order, Opts opts, Person current, boolean loadAll) {
    UUID pId = Conversions.ifUuid(portfolioId);

    if (pId != null) {
      QDbApplication queryApplicationList = new QDbApplication().portfolio.id.eq(pId);

      if (filter != null) {
        queryApplicationList = queryApplicationList.name.ilike("%" + filter + "%");
      }

      if (!opts.contains(FillOpts.Archived)) {
        queryApplicationList = queryApplicationList.whenArchived.isNull();
      }

      queryApplicationList = fetchApplicationOpts(opts, queryApplicationList);

      if (SortOrder.ASC == order) {
        queryApplicationList = queryApplicationList.order().name.asc();
      } else if (SortOrder.DESC == order) {
        queryApplicationList = queryApplicationList.order().name.desc();
      }

      if (!loadAll) {
        // we need to ascertain which apps they can actually see based on environments
        queryApplicationList = queryApplicationList.environments.groupRolesAcl.group.peopleInGroup.id.eq(Conversions.ifUuid(current.getId().getId()));
      }

      return queryApplicationList.findList().stream().map(app -> convertUtils.toApplication(app, opts)).collect(Collectors.toList());
    }

    return null;
  }

  private QDbApplication fetchApplicationOpts(Opts opts, QDbApplication eq) {
    if (opts.contains(FillOpts.Environments)) {
      eq = eq.environments.fetch();
    }

    if (opts.contains(FillOpts.Features)) {
      eq = eq.features.fetch();
    }

    if (!opts.contains(FillOpts.Archived)) {
      eq = eq.whenArchived.isNull();
    }

    return eq;
  }

  @Override
  public boolean deleteApplication(String portfolioId, String applicationId) {
    DbPortfolio portfolio = convertUtils.uuidPortfolio(portfolioId);

    if (portfolio != null) {
      DbApplication app = convertUtils.uuidApplication(applicationId);

      if (app != null && app.getPortfolio().getId().equals(portfolio.getId())) {
        archiveStrategy.archiveApplication(app);

        return true;
      }
    }

    return false;
  }

  @Override
  public Application getApplication(String appId, Opts opts) {
    return Conversions.uuid(appId)
      .map(aId -> convertUtils.toApplication(fetchApplicationOpts(opts, new QDbApplication().id.eq(aId)).findOne(), opts)).orElse(null);
  }

  @Override
  public Application updateApplication(String applicationId, Application application, Opts opts) throws DuplicateApplicationException, OptimisticLockingException {
    UUID appId = Conversions.ifUuid(applicationId);

    if (appId != null) {
      DbApplication app = fetchApplicationOpts(opts, new QDbApplication().id.eq(appId)).findOne();
      if (app != null) {
        if (application.getVersion() == null || application.getVersion() != app.getVersion()) {
          throw new OptimisticLockingException();
        }

        if (!app.getName().equals(application.getName())) {
          if (new QDbApplication().name.eq(application.getName()).whenArchived.isNull().findCount() > 0) {
            throw new DuplicateApplicationException();
          }
        }

        app.setName(application.getName());
        app.setDescription(application.getDescription());
        saveApp(app);

        return convertUtils.toApplication(app, opts);
      }
    }

    return null;
  }

  @Override
  public List<Feature> createApplicationFeature(String appId, Feature feature, Person person) throws DuplicateFeatureException {
    DbApplication app = convertUtils.uuidApplication(appId);

    if (app != null) {
      if (new QDbApplicationFeature().key.eq(feature.getKey()).parentApplication.eq(app).exists()) {
        throw new DuplicateFeatureException();
      }

      final DbApplicationFeature appFeature = new DbApplicationFeature.Builder()
        .name(feature.getName())
        .key(feature.getKey())
        .parentApplication(app)
        .alias(feature.getAlias())
        .link(feature.getLink())
        .secret(feature.getSecret() == null ? false : feature.getSecret())
        .valueType(feature.getValueType())
        .build();

      saveAppicationFeature(appFeature);

      cacheSource.publishFeatureChange(appFeature, PublishAction.CREATE);

      // if this is a boolean feature, create this feature with a default value of false in all environments we currently
      // have
      if (appFeature.getValueType() == FeatureValueType.BOOLEAN) {
        createDefaultBooleanFeatureValuesForAllEnvironments(appFeature, app, person);
      }

      return getAppFeatures(app);
    }

    return new ArrayList<>();
  }

  private void createDefaultBooleanFeatureValuesForAllEnvironments(DbApplicationFeature appFeature, DbApplication app, Person person) {
    final List<DbEnvironmentFeatureStrategy> newFeatures = new QDbEnvironment().whenArchived.isNull().parentApplication.eq(app).findList().stream().map(env -> new DbEnvironmentFeatureStrategy.Builder()
      .defaultValue(Boolean.FALSE.toString())
      .environment(env)
      .feature(appFeature)
      .featureState(FeatureState.ENABLED)
      .locked(true)
      .whoUpdated(convertUtils.uuidPerson(person))
      .build()).collect(Collectors.toList());

    saveAllFeatures(newFeatures);

    for (DbEnvironmentFeatureStrategy nf : newFeatures) {
      cacheSource.publishFeatureChange(nf);
    }
  }

  @Transactional
  private void saveAllFeatures(List<DbEnvironmentFeatureStrategy> newFeatures) {
    newFeatures.forEach(database::save);
  }

  private List<Feature> getAppFeatures(DbApplication app) {
    return app.getFeatures().stream().filter(af -> af.getWhenArchived() == null).map(af -> convertUtils.toApplicationFeature(af, Opts.empty())).collect(Collectors.toList());
  }

  @Override
  public List<Feature> updateApplicationFeature(String appId, String key, Feature feature) throws DuplicateFeatureException, OptimisticLockingException {
    DbApplication app = convertUtils.uuidApplication(appId);

    if (app != null) {
      DbApplicationFeature appFeature = new QDbApplicationFeature().and().key.eq(key).parentApplication.eq(app).endAnd().findOne();

      if (appFeature == null) {
        return null;
      }

      if (feature.getVersion() == null || appFeature.getVersion() != feature.getVersion()) {
        throw new OptimisticLockingException();
      }

      if (!key.equals(feature.getKey())) { // we are changing the key?
        if (new QDbApplicationFeature().key.eq(feature.getKey()).parentApplication.eq(app).endAnd().exists()) {
          throw new DuplicateFeatureException();
        }
      }

      appFeature.setName(feature.getName());
      appFeature.setAlias(feature.getAlias());
      appFeature.setKey(feature.getKey());
      appFeature.setLink(feature.getLink());
      appFeature.setValueType(feature.getValueType());
      appFeature.setSecret(feature.getSecret() == null ? false : feature.getSecret());

      updateApplicationFeature(appFeature);

      if (appFeature.getWhenArchived() == null) {
        cacheSource.publishFeatureChange(appFeature, PublishAction.UPDATE);
      }

      return getAppFeatures(app);
    }
    return new ArrayList<>();
  }

  @Transactional
  private void updateApplicationFeature(DbApplicationFeature appFeature) {
    database.update(appFeature);
  }

  @Transactional
  private void saveAppicationFeature(DbApplicationFeature f) {
    database.save(f);
  }

  @Override
  public List<Feature> getApplicationFeatures(String appId) {
    DbApplication app = convertUtils.uuidApplication(appId);

    if (app != null) {
      return getAppFeatures(app);
    }

    return new ArrayList<>();
  }

  static class AppFeature {
    final DbApplication app;
    final DbApplicationFeature appFeature;

    public AppFeature(DbApplication app, DbApplicationFeature appFeature) {
      this.app = app;
      this.appFeature = appFeature;
    }

    boolean isValid() {
      return app != null && appFeature != null;
    }
  }
  private AppFeature findAppFeature(String appId, String key) {
    DbApplication app = convertUtils.uuidApplication(appId);

    if (app != null) {
      DbApplicationFeature appFeature = new QDbApplicationFeature().and().key.eq(key).parentApplication.eq(app).endAnd().findOne();

      if (appFeature == null) {
        UUID id = Conversions.ifUuid(key);
        if (id != null) {
          return new AppFeature(app, new QDbApplicationFeature().id.eq(id).parentApplication.eq(app).findOne());
        }
      } else {
        return new AppFeature(app, appFeature);
      }
    }

    return null;
  }

  @Override
  public List<Feature> deleteApplicationFeature(String appId, String key) {
    AppFeature appFeature = findAppFeature(appId, key);

    if (appFeature == null || !appFeature.isValid()) {
      return null;
    }

    // make sure it isn't already deleted
    if (appFeature.appFeature.getWhenArchived() == null) {
      archiveStrategy.archiveApplicationFeature(appFeature.appFeature);
    }

    return getAppFeatures(appFeature.app);
  }

  @Override
  public Feature getApplicationFeatureByKey(String appId, String key) {
    AppFeature af = findAppFeature(appId, key);

    if (af == null || !af.isValid()) {
      return null;
    }

    return convertUtils.toApplicationFeature(af.appFeature, Opts.empty());
  }

  // finds all of the groups attached to this application  that have application roles
  // and filters them by the feature edit role, and adds them to the outgoing set.
  @Override
  public Set<String> findFeatureEditors(String id) {

    UUID appId = Conversions.ifUuid(id);

    Set<String> featureEditors = new HashSet<>();

    if (appId != null) {
      new QDbAcl().application.id.eq(appId).group.whenArchived.isNull().group.peopleInGroup.fetch().findList().forEach(acl -> {
        ApplicationGroupRole agr = convertUtils.applicationGroupRoleFromAcl(acl);

        if (agr.getRoles().contains(ApplicationRoleType.FEATURE_EDIT)) {
          acl.getGroup().getPeopleInGroup().forEach(p -> {
            featureEditors.add(p.getId().toString());
          });
        }
      });
    }

    return featureEditors;
  }

  public Set<String> findFeatureReaders(String id) {
    UUID appId = Conversions.ifUuid(id);
    Set<String> featureReaders = new HashSet<>();

    if (appId != null) {
      new QDbAcl()
        .environment.parentApplication.id.eq(appId)
        .group.whenArchived.isNull()
        .group.peopleInGroup.fetch().findList().forEach(acl -> {
        if (acl.getRoles().trim().length() > 0) {
          acl.getGroup().getPeopleInGroup().forEach(p ->
            featureReaders.add(p.getId().toString()));
        }
      });
    }

    return featureReaders;
  }

  public boolean personIsFeatureReader(String appId, String personId) {
    UUID applicationId = Conversions.ifUuid(appId);
    DbPerson person = convertUtils.uuidPerson(personId);

    if (convertUtils.personIsSuperAdmin(person)) {
      return true;
    }

    if (applicationId != null && person != null) {

      for(DbAcl acl : new QDbAcl()
        .or()
        .environment.parentApplication.id.eq(applicationId)
        .application.id.eq(applicationId)
        .endOr()
        .group.whenArchived.isNull()
        .group.peopleInGroup.eq(person).findList()) {
        if (acl.getApplication() != null) {
          return true;
        }
        if (acl.getRoles().trim().length() > 0) {
          return true;
        }
      }
    }

    return false;
  }
}
