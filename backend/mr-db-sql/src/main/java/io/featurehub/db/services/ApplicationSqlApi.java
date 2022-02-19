package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.featurehub.dacha.model.PublishAction;
import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbFeatureValue;
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
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public ApplicationSqlApi(
      Database database,
      Conversions convertUtils,
      CacheSource cacheSource,
      ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public Application createApplication(@NotNull UUID portfolioId, @NotNull Application application, @NotNull Person current)
      throws DuplicateApplicationException {
    Conversions.nonNullPortfolioId(portfolioId);
    Conversions.nonNullPerson(current);

    DbPortfolio portfolio = convertUtils.byPortfolio(portfolioId);

    if (portfolio != null) {
      DbPerson updater = convertUtils.byPerson(current);

      if (updater != null) {
        if (!new QDbApplication()
                .and()
                .name
                .iequalTo(application.getName())
                .whenArchived
                .isNull()
                .portfolio
                .eq(portfolio)
                .endAnd()
                .exists()
            ) {
          DbApplication aApp =
              new DbApplication.Builder()
                  .name(application.getName())
                  .description(application.getDescription())
                  .portfolio(portfolio)
                  .whoCreated(updater)
                  .build();

          // update the portfolio group to ensure it has permissions to add features to this new
          // application
          final DbGroup adminGroup =
              new QDbGroup()
                  .owningPortfolio
                  .eq(aApp.getPortfolio())
                  .whenArchived
                  .isNull()
                  .adminGroup
                  .isTrue()
                  .findOne();
          if (adminGroup != null) {
            adminGroup
                .getGroupRolesAcl()
                .add(
                    new DbAcl.Builder()
                        .application(aApp)
                        .roles(
                            GroupSqlApi.appRolesToString(
                                Collections.singletonList(ApplicationRoleType.FEATURE_EDIT)))
                        .build());
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
  private void addApplicationFeatureCreationRoleToPortfolioAdminGroup(
      DbApplication aApp, DbGroup adminGroup) {
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
  public @NotNull List<Application> findApplications(
    @NotNull UUID portfolioId,
    String filter,
    SortOrder order,
    @NotNull Opts opts,
    @NotNull Person current,
    boolean loadAll) {
    Conversions.nonNullPortfolioId(portfolioId);

    QDbApplication queryApplicationList = new QDbApplication().portfolio.id.eq(portfolioId);

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
      Conversions.nonNullPerson(current);
      // we need to ascertain which apps they can actually see based on environments
      queryApplicationList =
          queryApplicationList.environments.groupRolesAcl.group.peopleInGroup.id.eq(
              current.getId().getId());
    }

    return queryApplicationList.findList().stream()
        .map(app -> convertUtils.toApplication(app, opts))
        .collect(Collectors.toList());
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
  public boolean deleteApplication(@NotNull UUID portfolioId, UUID applicationId) {
    Conversions.nonNullPortfolioId(portfolioId);
    Conversions.nonNullApplicationId(applicationId);

    DbPortfolio portfolio = convertUtils.byPortfolio(portfolioId);

    if (portfolio != null) {
      DbApplication app = convertUtils.byApplication(applicationId);

      if (app != null && app.getPortfolio().getId().equals(portfolio.getId())) {
        archiveStrategy.archiveApplication(app);

        return true;
      }
    }

    return false;
  }

  @Override
  public Application getApplication(@NotNull UUID appId, @NotNull Opts opts) {
    Conversions.nonNullApplicationId(appId);

    return convertUtils.toApplication(
        fetchApplicationOpts(opts, new QDbApplication().id.eq(appId)).findOne(), opts);
  }

  @Override
  public Application updateApplication(@NotNull UUID applicationId, @NotNull Application application, @NotNull Opts opts)
      throws DuplicateApplicationException, OptimisticLockingException {
    Conversions.nonNullApplicationId(applicationId);

    DbApplication app =
        fetchApplicationOpts(opts, new QDbApplication().id.eq(applicationId)).findOne();
    if (app != null) {
      if (application.getVersion() == null || application.getVersion() != app.getVersion()) {
        throw new OptimisticLockingException();
      }

      if (!app.getName().equals(application.getName())) {
        if (new QDbApplication()
                .portfolio
                .eq(app.getPortfolio())
                .name
                .eq(application.getName())
                .whenArchived
                .isNull()
                .exists()) {
          throw new DuplicateApplicationException();
        }
      }

      app.setName(application.getName());
      app.setDescription(application.getDescription());
      saveApp(app);

      return convertUtils.toApplication(app, opts);
    }

    return null;
  }

  @Override
  public List<Feature> createApplicationFeature(@NotNull UUID applicationId, Feature feature, Person person,
                                                @NotNull Opts opts)
      throws DuplicateFeatureException {
    Conversions.nonNullApplicationId(applicationId);

    DbApplication app = convertUtils.byApplication(applicationId);

    if (app != null) {
      if (new QDbApplicationFeature().key.eq(feature.getKey()).parentApplication.eq(app).exists()) {
        throw new DuplicateFeatureException();
      }

      final DbApplicationFeature appFeature =
          new DbApplicationFeature.Builder()
              .name(feature.getName())
              .key(feature.getKey())
              .parentApplication(app)
              .alias(feature.getAlias())
              .link(feature.getLink())
              .secret(feature.getSecret() != null && feature.getSecret())
              .valueType(feature.getValueType())
              .metaData(feature.getMetaData())
              .description(feature.getDescription())
              .build();

      saveApplicationFeature(appFeature);

      if (appFeature.getValueType() != FeatureValueType.BOOLEAN) {
        cacheSource.publishFeatureChange(appFeature, PublishAction.CREATE);
      }

      // if this is a boolean feature, create this feature with a default value of false in all
      // environments we currently
      // have
      if (appFeature.getValueType() == FeatureValueType.BOOLEAN) {
        createDefaultBooleanFeatureValuesForAllEnvironments(appFeature, app, person);
      }

      return getAppFeatures(app, opts);
    }

    return new ArrayList<>();
  }

  private void createDefaultBooleanFeatureValuesForAllEnvironments(
      DbApplicationFeature appFeature, DbApplication app, Person person) {
    final List<DbFeatureValue> newFeatures =
        new QDbEnvironment()
            .whenArchived.isNull().parentApplication.eq(app).findList().stream()
                .map(
                    env ->
                        new DbFeatureValue.Builder()
                            .defaultValue(Boolean.FALSE.toString())
                            .environment(env)
                            .feature(appFeature)
                            .featureState(FeatureState.ENABLED)
                            .locked(true)
                            .whoUpdated(convertUtils.byPerson(person))
                            .build())
                .collect(Collectors.toList());

    saveAllFeatures(newFeatures);

    cacheSource.publishFeatureChange(appFeature, PublishAction.CREATE);
  }

  @Transactional
  private void saveAllFeatures(List<DbFeatureValue> newFeatures) {
    newFeatures.forEach(database::save);
  }

  private List<Feature> getAppFeatures(DbApplication app, @NotNull Opts opts) {
    return app.getFeatures().stream()
        .filter(af -> af.getWhenArchived() == null)
        .map(af -> convertUtils.toApplicationFeature(af, opts))
        .collect(Collectors.toList());
  }

  @Override
  public List<Feature> updateApplicationFeature(UUID appId, String key, Feature feature, @NotNull Opts opts)
      throws DuplicateFeatureException, OptimisticLockingException {
    Conversions.nonNullApplicationId(appId);

    DbApplication app = convertUtils.byApplication(appId);

    if (app != null) {
      DbApplicationFeature appFeature =
          new QDbApplicationFeature()
              .and()
              .key
              .eq(key)
              .parentApplication
              .eq(app)
              .endAnd()
              .findOne();

      if (appFeature == null) {
        return null;
      }

      if (feature.getVersion() == null || appFeature.getVersion() != feature.getVersion()) {
        throw new OptimisticLockingException();
      }

      if (!key.equals(feature.getKey())) { // we are changing the key?
        if (new QDbApplicationFeature()
            .key
            .eq(feature.getKey())
            .parentApplication
            .eq(app)
            .endAnd()
            .exists()) {
          throw new DuplicateFeatureException();
        }
      }

      boolean changed =
        (feature.getKey() != null && !feature.getKey().equals(appFeature.getKey())) || feature.getValueType() != appFeature.getValueType();

      appFeature.setName(feature.getName());
      appFeature.setAlias(feature.getAlias());

      if (feature.getKey() != null) {
        appFeature.setKey(feature.getKey());
      }

      if (feature.getLink() != null) {
        appFeature.setLink(feature.getLink());
      }

      if (feature.getValueType() != null) {
        appFeature.setValueType(feature.getValueType());
      }

      appFeature.setSecret(feature.getSecret() != null && feature.getSecret());

      if (appFeature.getMetaData() != null) {
        appFeature.setMetaData(feature.getMetaData());
      }

      if (appFeature.getDescription() != null) {
        appFeature.setDescription(feature.getDescription());
      }

      updateApplicationFeature(appFeature);

      if (appFeature.getWhenArchived() == null && changed) {
        cacheSource.publishFeatureChange(appFeature, PublishAction.UPDATE);
      }

      return getAppFeatures(app, opts);
    }

    return new ArrayList<>();
  }

  @Transactional
  private void updateApplicationFeature(DbApplicationFeature appFeature) {
    database.update(appFeature);
  }

  @Transactional
  private void saveApplicationFeature(DbApplicationFeature f) {
    database.save(f);
  }

  @Override
  public List<Feature> getApplicationFeatures(@NotNull UUID appId, @NotNull Opts opts) {
    Conversions.nonNullApplicationId(appId);

    DbApplication app = convertUtils.byApplication(appId);

    if (app != null) {
      return getAppFeatures(app, opts);
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

  private AppFeature findAppFeature(UUID appId, String applicationFeatureKeyName) {
    Conversions.nonNullApplicationId(appId);

    DbApplication app = convertUtils.byApplication(appId);

    if (app != null) {
      DbApplicationFeature appFeature =
          new QDbApplicationFeature()
              .and()
              .key
              .eq(applicationFeatureKeyName)
              .parentApplication
              .eq(app)
              .endAnd()
              .findOne();

      if (appFeature == null) {
        UUID id = Conversions.checkUuid(applicationFeatureKeyName);
        if (id != null) {
          return new AppFeature(
              app, new QDbApplicationFeature().id.eq(id).parentApplication.eq(app).findOne());
        }
      } else {
        return new AppFeature(app, appFeature);
      }
    }

    return null;
  }

  @Override
  public List<Feature> deleteApplicationFeature(UUID appId, String key) {
    Conversions.nonNullApplicationId(appId);

    AppFeature appFeature = findAppFeature(appId, key);

    if (appFeature == null || !appFeature.isValid()) {
      return null;
    }

    // make sure it isn't already deleted
    if (appFeature.appFeature.getWhenArchived() == null) {
      archiveStrategy.archiveApplicationFeature(appFeature.appFeature);
    }

    return getAppFeatures(appFeature.app, Opts.empty());
  }

  @Override
  public Feature getApplicationFeatureByKey(UUID appId, @NotNull String key, @NotNull Opts opts) {
    Conversions.nonNullApplicationId(appId);

    AppFeature af = findAppFeature(appId, key);

    if (af == null || !af.isValid()) {
      return null;
    }

    return convertUtils.toApplicationFeature(af.appFeature, opts);
  }

  // finds all of the groups attached to this application  that have application roles
  // and filters them by the feature edit role, and adds them to the outgoing set.
  @Override
  public Set<UUID> findFeatureEditors(UUID appId) {
    Conversions.nonNullApplicationId(appId);

    Set<UUID> featureEditors = new HashSet<>();

    new QDbAcl()
        .application
        .id
        .eq(appId)
        .group
        .whenArchived
        .isNull()
        .group
        .peopleInGroup
        .fetch()
        .findList()
        .forEach(
            acl -> {
              ApplicationGroupRole agr = convertUtils.applicationGroupRoleFromAcl(acl);

              if (agr.getRoles().contains(ApplicationRoleType.FEATURE_EDIT)) {
                acl.getGroup()
                    .getPeopleInGroup()
                    .forEach(
                        p -> {
                          featureEditors.add(p.getId());
                        });
              }
            });

    return featureEditors;
  }

  public Set<UUID> findFeatureReaders(UUID appId) {
    Conversions.nonNullApplicationId(appId);
    Set<UUID> featureReaders = new HashSet<>();

    new QDbAcl()
        .or()
        .environment
        .parentApplication
        .id
        .eq(appId)
        .application
        .id
        .eq(appId)
        .endOr()
        .group
        .whenArchived
        .isNull()
        .group
        .peopleInGroup
        .fetch()
        .findList()
        .forEach(
            acl -> {
              if (acl.getApplication() != null || acl.getRoles().trim().length() > 0) {
                acl.getGroup()
                    .getPeopleInGroup()
                    .forEach(p -> featureReaders.add(p.getId()));
              }
            });

    // we don't need to add superusers because they are automatically added to each portfolio group

    return featureReaders;
  }

  public boolean personIsFeatureReader(UUID applicationId, UUID personId) {
    Conversions.nonNullApplicationId(applicationId);
    Conversions.nonNullPersonId(personId);

    DbPerson person = convertUtils.byPerson(personId);

    if (convertUtils.personIsSuperAdmin(person)) {
      return true;
    }

    if (person != null) {
      for (DbAcl acl :
          new QDbAcl()
              .or()
              .environment
              .parentApplication
              .id
              .eq(applicationId)
              .application
              .id
              .eq(applicationId)
              .endOr()
              .group
              .whenArchived
              .isNull()
              .group
              .peopleInGroup
              .eq(person)
              .findList()) {
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
