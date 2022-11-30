package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.annotation.Transactional;
import io.ebean.annotation.TxType;
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
import io.featurehub.db.model.query.QDbGroupMember;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.mr.events.common.CacheSource;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
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
  private final InternalFeatureSqlApi internalFeatureSqlApi;

  @Inject
  public ApplicationSqlApi(
      Database database,
      Conversions convertUtils,
      CacheSource cacheSource,
      ArchiveStrategy archiveStrategy,
      InternalFeatureSqlApi internalFeatureSqlApi) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.cacheSource = cacheSource;
    this.archiveStrategy = archiveStrategy;
    this.internalFeatureSqlApi = internalFeatureSqlApi;
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
                            GroupSqlApi.appRolesToString(Arrays.asList(ApplicationRoleType.CREATE,
                              ApplicationRoleType.EDIT_AND_DELETE)))
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
          queryApplicationList
              .environments.groupRolesAcl
              .group.groupMembers.person.id.eq(current.getId().getId());

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

  public @Nullable Application getApplication(@NotNull UUID portfolioId, @NotNull String name) {
    final DbApplication app = new QDbApplication().name.ieq(name).portfolio.id.eq(portfolioId).findOne();

    if (app != null) {
      return convertUtils.toApplication(app, Opts.empty());
    }

    return null;
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

  // this ensures we create the featuers and create initial historical records for them as well
  @Transactional(type = TxType.REQUIRES_NEW)
  private void saveAllFeatures(List<DbFeatureValue> newFeatures) {
    newFeatures.forEach(internalFeatureSqlApi::saveFeatureValue);
  }

  private List<Feature> getAppFeatures(DbApplication app, @NotNull Opts opts) {
    return app.getFeatures().stream()
        .filter(af -> af.getWhenArchived() == null)
        .map(af -> convertUtils.toApplicationFeature(af, opts))
        .collect(Collectors.toList());
  }

  @Override
  public List<Feature> updateApplicationFeature(@NotNull UUID appId, String key, Feature feature, @NotNull Opts opts)
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

      if (feature.getMetaData() != null) {
        appFeature.setMetaData(feature.getMetaData());
      }

      if (feature.getDescription() != null) {
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
  public List<Feature> deleteApplicationFeature(@NotNull UUID appId, String key) {
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
  public Feature getApplicationFeatureByKey(@NotNull UUID appId, @NotNull String key, @NotNull Opts opts) {
    Conversions.nonNullApplicationId(appId);

    AppFeature af = findAppFeature(appId, key);

    if (af == null || !af.isValid()) {
      return null;
    }

    return convertUtils.toApplicationFeature(af.appFeature, opts);
  }

  final Set<ApplicationRoleType> editorRoles = Set.of(ApplicationRoleType.EDIT, ApplicationRoleType.EDIT_AND_DELETE);
  final Set<ApplicationRoleType> creatorRoles = Set.of(ApplicationRoleType.EDIT, ApplicationRoleType.CREATE,
    ApplicationRoleType.EDIT_AND_DELETE);

  // finds all of the groups attached to this application  that have application roles
  // and filters them by the feature edit role, and adds them to the outgoing set.
  @Override
  public @NotNull Set<UUID> findFeatureEditors(@NotNull UUID appId) {
    Conversions.nonNullApplicationId(appId);

    return findFeaturePermissionsByType(appId, editorRoles);
  }

  @Override
  public @NotNull Set<UUID> findFeatureCreators(@NotNull UUID appId) {
    Conversions.nonNullApplicationId(appId);

    return findFeaturePermissionsByType(appId, creatorRoles);
  }

  @Override
  public boolean personIsFeatureEditor(@NotNull UUID appId, @NotNull UUID personId) {
    return personHoldsOneOfApplicationRoles(appId, personId, editorRoles);
  }

  @Override
  public boolean personIsFeatureCreator(@NotNull UUID appId, @NotNull UUID personId) {
    return personHoldsOneOfApplicationRoles(appId, personId, creatorRoles);
  }

  private boolean personHoldsOneOfApplicationRoles(@NotNull UUID appId,
                                                   @NotNull UUID personId, Set<ApplicationRoleType> roles) {
    return new QDbAcl()
      .select(QDbAcl.Alias.roles)
      .application.id.eq(appId)
      .group.groupMembers.person.id.eq(personId)
      .findList().stream().anyMatch(acl ->
      convertUtils.splitApplicationRoles(acl.getRoles()).stream().anyMatch(roles::contains)
    );
  }

  private Set<UUID> findFeaturePermissionsByType(UUID appId, Set<ApplicationRoleType> roles) {
    log.info("searching for permissions for app id {}, roles {}", appId, roles);
    // find which groups have those roles
    final List<UUID> groups = new QDbAcl()
      .select(QDbAcl.Alias.group.id, QDbAcl.Alias.roles)
      .application.id.eq(appId)
      .group.whenArchived.isNull()
      .findList().stream().filter(acl ->
         convertUtils.splitApplicationRoles(acl.getRoles()).stream().anyMatch(roles::contains)
      ).map(acl -> acl.getGroup().getId()).collect(Collectors.toList());

    // find which people are in those groups
    if (!groups.isEmpty()) {
      final Set<UUID> collect = new QDbPerson()
        .select(QDbPerson.Alias.id)
        .groupMembers.group.id.in(groups).findList().stream().map(DbPerson::getId).collect(Collectors.toSet());
      log.info("and out with {}", collect);
      return collect;
    }

    return new HashSet<>();
  }

  public @NotNull Set<UUID> findFeatureReaders(@NotNull UUID appId) {
    Conversions.nonNullApplicationId(appId);
    Set<UUID> featureReaders = new HashSet<>();

    new QDbAcl()
      .or()
        .environment.parentApplication.id.eq(appId)
        .application.id.eq(appId)
      .endOr()
      .group.whenArchived.isNull()
      .group.groupMembers.person.fetch()
      .findList()
      .forEach(acl -> {
              if (acl.getApplication() != null || acl.getRoles().trim().length() > 0) {
                acl.getGroup()
                    .getGroupMembers()
                    .forEach(p -> featureReaders.add(p.getPerson().getId()));
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
              .groupMembers.person
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
