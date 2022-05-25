package io.featurehub.db.services;

import io.featurehub.db.FilterOptType;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbNamedCache;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.DbServiceAccountEnvironment;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbApplication;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.model.query.QDbLogin;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.model.query.QDbPortfolio;
import io.featurehub.db.model.query.QDbRolloutStrategy;
import io.featurehub.db.model.query.QDbServiceAccountEnvironment;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.ApplicationGroupRole;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.EnvironmentGroupRole;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.RoleType;
import io.featurehub.mr.model.RolloutStrategyInfo;
import io.featurehub.mr.model.RolloutStrategyInstance;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ConvertUtils implements Conversions {
  private static final Logger log = LoggerFactory.getLogger(ConvertUtils.class);

  @Override
  public DbPerson byPerson(UUID personId) {
    return personId == null ? null : new QDbPerson().id.eq(personId).findOne();
  }

  @Override
  public DbPerson byPerson(UUID personId, Opts opts) {
    if (personId == null) {
      return null;
    }

    QDbPerson finder = new QDbPerson().id.eq(personId);
    if (opts.contains(FillOpts.Groups)) {
      finder = finder.groupMembers.fetch();
    }

    return finder.findOne();
  }

  @Override
  public DbPortfolio byPortfolio(UUID pId) {
    return pId == null ? null : new QDbPortfolio().id.eq(pId).findOne();
  }

  @Override
  public DbEnvironment byEnvironment(UUID eId) {
    return eId == null ? null : new QDbEnvironment().id.eq(eId).findOne();
  }

  @Override
  public DbEnvironment byEnvironment(UUID id, Opts opts) {
    if (id == null) {
      return null;
    }

    final QDbEnvironment eq = new QDbEnvironment().id.eq(id);

    if (opts.contains(FillOpts.Applications)) {
      eq.parentApplication.fetch();
    }
    if (opts.contains(FillOpts.Portfolios)) {
      eq.parentApplication.portfolio.fetch();
    }
    if (opts.contains(FillOpts.ApplicationIds)) {
      eq.parentApplication.fetch(QDbApplication.Alias.id);
    }
    if (opts.contains(FillOpts.PortfolioIds)) {
      eq.parentApplication.portfolio.fetch(QDbPortfolio.Alias.id);
    }

    return eq.findOne();
  }

  @Override
  public DbApplication byApplication(UUID id) {
    return id == null ? null : new QDbApplication().id.eq(id).findOne();
  }

  @Override
  public boolean personIsNotSuperAdmin(DbPerson person) {
    return !new QDbGroup()
            .owningPortfolio
            .isNull()
            .adminGroup
            .isTrue()
            .groupMembers.person
            .id
            .eq(person.getId())
            .exists();
  }

  @Override
  public boolean personIsSuperAdmin(DbPerson person) {
    return new QDbGroup()
            .whenArchived
            .isNull()
            .owningPortfolio
            .isNull()
            .groupMembers.person
            .eq(person)
            .adminGroup
            .isTrue()
            .exists();
  }

  @Override
  public String limitLength(String s, int len) {
    return s == null ? null : (s.length() > len ? s.substring(0, len) : s);
  }

  @Override
  public Environment toEnvironment(
      DbEnvironment env, Opts opts, Set<DbApplicationFeature> features) {
    if (env == null) {
      return null;
    }

    Environment environment =
        new Environment()
            .id(env.getId())
            .name(stripArchived(env.getName(), env.getWhenArchived()))
            .version(env.getVersion())
            .production(env.isProductionEnvironment())
            .priorEnvironmentId(
                env.getPriorEnvironment() != null ? env.getPriorEnvironment().getId() : null)
            .applicationId(env.getParentApplication().getId());

    if (opts.contains(FillOpts.People)) {
      environment.updatedBy(
          toPerson(
              env.getWhoCreated(),
              env.getParentApplication().getPortfolio().getOrganization(),
              Opts.empty()));
      environment.createdBy(toPerson(env.getWhoCreated()));
    }

    if (opts.contains(FillOpts.Features)) {
      if (features != null) {
        environment.setFeatures(
            features.stream()
                .map(ef -> toApplicationFeature(ef, Opts.empty()))
                .collect(Collectors.toList()));
      } else {
        environment.setFeatures(
            env.getEnvironmentFeatures().stream()
                .filter(
                    f ->
                        opts.contains(FillOpts.Archived)
                            || f.getFeature().getWhenArchived() == null)
                .map(ef -> toApplicationFeature(ef.getFeature(), Opts.empty()))
                .collect(Collectors.toList()));
      }
    }

    if (opts.contains(FillOpts.ServiceAccounts) || opts.contains(FillOpts.SdkURL)) {
      environment.setServiceAccountPermission(
          env.getServiceAccountEnvironments().stream()
              .filter(
                  sae ->
                      opts.contains(FillOpts.Archived)
                          || sae.getServiceAccount().getWhenArchived() == null)
              .map(sae -> toServiceAccountPermission(sae, null, false, opts))
              .collect(Collectors.toList()));
    }

    // collect all of the ACls for all of the groups for this environment?
    if (opts.contains(FillOpts.Acls)) {
      new QDbAcl()
          .environment
          .eq(env)
          .findEach(acl -> environment.addGroupRolesItem(environmentGroupRoleFromAcl(acl)));
    }

    return environment;
  }

  @Override
  public Environment toEnvironment(DbEnvironment env, Opts opts) {
    return toEnvironment(env, opts, null);
  }

  @Override
  public String getCacheNameByEnvironment(DbEnvironment env) {
    return new QDbNamedCache()
        .organizations
        .portfolios
        .applications
        .environments
        .eq(env)
        .findOneOrEmpty()
        .map(DbNamedCache::getCacheName)
        .orElse(null);
  }

  @Override
  public ServiceAccountPermission toServiceAccountPermission(
      DbServiceAccountEnvironment sae,
      Set<RoleType> rolePerms,
      boolean mustHaveRolePerms,
      Opts opt) {
    final ServiceAccountPermission sap =
        new ServiceAccountPermission()
            .id(sae.getId())
            .permissions(splitServiceAccountPermissions(sae.getPermissions()))
            .environmentId(sae.getEnvironment().getId());

    if (sap.getPermissions().isEmpty() && opt.contains(FillOpts.IgnoreEmptyPermissions)) {
      return null;
    }

    // if they don't have read, but they really do have read, add read
    if (!sap.getPermissions().isEmpty() && !sap.getPermissions().contains(RoleType.READ)) {
      sap.getPermissions().add(RoleType.READ);
    }

    if (opt.contains(FillOpts.ServiceAccounts) || (opt.contains(FillOpts.SdkURL) && !opt.contains(FillOpts.ServiceAccountPermissionFilter)) ) {
      sap.serviceAccount(
          toServiceAccount(
              sae.getServiceAccount(), opt.minus(FillOpts.Permissions, FillOpts.SdkURL)));
    }

    if (opt.contains(FillOpts.SdkURL)) {
      // if role perms is null (i.e we don't care) or the roles that a person has is a super-set of
      // the roles of the service account
      if (!mustHaveRolePerms
          || (rolePerms != null && rolePerms.containsAll(sap.getPermissions()))) {
        String cacheName = getCacheNameByEnvironment(sae.getEnvironment());
        sap.sdkUrlClientEval(
            String.format(
                "%s/%s/%s",
                cacheName, sap.getEnvironmentId(), sae.getServiceAccount().getApiKeyClientEval()));
        sap.sdkUrlServerEval(
            String.format(
                "%s/%s/%s",
                cacheName, sap.getEnvironmentId(), sae.getServiceAccount().getApiKeyServerEval()));
      }
    }

    return sap;
  }

  @Override
  public ApplicationGroupRole applicationGroupRoleFromAcl(DbAcl acl) {
    return new ApplicationGroupRole()
        .groupId(acl.getGroup().getId())
        .roles(splitApplicationRoles(acl.getRoles()))
        .applicationId(acl.getApplication().getId());
  }

  @Override
  public EnvironmentGroupRole environmentGroupRoleFromAcl(DbAcl acl) {
    final EnvironmentGroupRole environmentGroupRole =
        new EnvironmentGroupRole()
            .groupId(acl.getGroup().getId())
            .roles(splitEnvironmentRoles(acl.getRoles()))
            .environmentId(acl.getEnvironment().getId());

    // READ should be implicit if we have any of the other roles
    if (!environmentGroupRole.getRoles().contains(RoleType.READ)
        && !environmentGroupRole.getRoles().isEmpty()) {
      environmentGroupRole.addRolesItem(RoleType.READ);
    }

    return environmentGroupRole;
  }

  @Override
  public @NotNull List<RoleType> splitEnvironmentRoles(String roles) {
    Set<RoleType> roleTypes = new HashSet<>();
    if (roles == null || roles.length() == 0) {
      return new ArrayList<>(roleTypes);
    }

    for (String n : roles.split(",")) {
      try {
        roleTypes.add(RoleType.valueOf(n));
      } catch (Exception ignored) {
      }
    }

    return new ArrayList<>(roleTypes);
  }

  @Override
  public List<ApplicationRoleType> splitApplicationRoles(String roles) {
    Set<ApplicationRoleType> roleTypes = new HashSet<>();

    if (roles != null) {
      for (String n : roles.split(",")) {
        try {
          roleTypes.add(ApplicationRoleType.valueOf(n));
        } catch (Exception e) {
          return null;
        }
      }
    }

    return new ArrayList<>(roleTypes);
  }

  @Override
  public EnvironmentGroupRole convertEnvironmentAcl(DbAcl dbAcl) {
    return new EnvironmentGroupRole()
        .environmentId(dbAcl.getEnvironment().getId())
        .groupId(dbAcl.getGroup().getId());
  }

  @Override
  public OffsetDateTime toOff(LocalDateTime ldt) {
    return ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
  }

  @NotNull @Override public String personName(@NotNull DbPerson person) {
    if (person.getName() == null || person.getName().isEmpty()) {
      return "No name";
    }

    return person.getName();
  }

  @Override
  public Person toPerson(DbPerson person) {
    if (person == null) {
      return null;
    }

    return new Person()
        .id(new PersonId().id(person.getId()))
        .version(person.getVersion())
        .passwordRequiresReset(person.isPasswordRequiresReset())
        .email(person.getEmail())
        .personType(person.getPersonType())
        .name(personName(person))
        .groups(null);
  }

  public UUID getOrganizationId() {
    return getDbOrganization().getId();
  }

  public DbOrganization getDbOrganization() {
    return new QDbOrganization().findOne();
  }

  @Override
  public Person toPerson(DbPerson dbp, @NotNull Opts opts) {
    return toPerson(dbp, null, opts);
  }

  @Override
  public Person toPerson(DbPerson dbp, DbOrganization org, @NotNull Opts opts) {
    if (dbp == null) {
      return null;
    }

    if (opts.contains(FillOpts.SimplePeople)) {
      return toPerson(dbp);
    }

    Person p =
        new Person()
            .email(dbp.getEmail())
            .name(stripArchived(personName(dbp), dbp.getWhenArchived()))
            .version(dbp.getVersion())
            .passwordRequiresReset(dbp.isPasswordRequiresReset())
            .personType(dbp.getPersonType())
            .whenArchived(toOff(dbp.getWhenArchived()))
            .id(new PersonId().id(dbp.getId()));

    if (opts.contains(FillOpts.PersonLastLoggedIn)) {
      if (dbp.getWhenLastAuthenticated() != null) {
        p.whenLastAuthenticated(dbp.getWhenLastAuthenticated().atOffset(ZoneOffset.UTC));
      }

      new QDbLogin().person.id.eq(dbp.getId()).orderBy().lastSeen.desc().setMaxRows(1).findList().forEach(login -> {
        p.whenLastSeen(login.getLastSeen().atOffset(ZoneOffset.UTC));
      });
    }

    if (opts.contains(FillOpts.Groups)) {
      final List<DbGroup> groupList = new QDbGroup()
        .whenArchived
        .isNull()
        .groupMembers.person
        .eq(dbp)
        .owningOrganization
        .id.eq(org == null ? getOrganizationId() : org.getId())
        .findList();

      log.info("groups for person {} are {}", p, groupList);

      groupList
          .forEach(dbg -> p.addGroupsItem(toGroup(dbg, opts.minus(FillOpts.Groups))));
    }

    return p;
  }

  @Override
  public Group toGroup(DbGroup dbg, Opts opts) {
    if (dbg == null) {
      return null;
    }

    Group group = new Group().version(dbg.getVersion()).whenArchived(toOff(dbg.getWhenArchived()));
    group.setId(dbg.getId());
    group.setName(stripArchived(dbg.getName(), dbg.getWhenArchived()));
    group.setAdmin(dbg.isAdminGroup());
    if (dbg.getOwningPortfolio() != null) {
      group.setPortfolioId(dbg.getOwningPortfolio().getId());
    }
    group.setOrganizationId(
        dbg.getOwningOrganization() == null ? null : dbg.getOwningOrganization().getId());

    if (opts.contains(FillOpts.Members)) {
      DbOrganization org =
          dbg.getOwningOrganization() == null
              ? dbg.getOwningPortfolio().getOrganization()
              : dbg.getOwningOrganization();
      group.setMembers(
          new QDbPerson()
              .order().name.asc().whenArchived.isNull().groupMembers.group.eq(dbg).findList().stream()
                  .map(
                      p ->
                          this.toPerson(
                              p, org, opts.minus(FillOpts.Members, FillOpts.Acls, FillOpts.Groups)))
                  .collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Acls)) {
      UUID appIdFilter = opts.id(FilterOptType.Application);

      QDbAcl aclQuery = new QDbAcl().group.eq(dbg);

      if (appIdFilter != null) {
        aclQuery =
            aclQuery
                .or()
                .environment
                .parentApplication
                .id
                .eq(appIdFilter)
                .application
                .id
                .eq(appIdFilter)
                .endOr();
      }

      aclQuery.findEach(
          acl -> {
            if (acl.getEnvironment() != null) {
              group.addEnvironmentRolesItem(environmentGroupRoleFromAcl(acl));
            } else if (acl.getApplication() != null) {
              group.addApplicationRolesItem(applicationGroupRoleFromAcl(acl));
            }
          });
    }

    return group;
  }

  @Override
  public Application toApplication(DbApplication app, Opts opts) {
    if (app == null) {
      return null;
    }

    Application application =
        new Application()
            .name(stripArchived(app.getName(), app.getWhenArchived()))
            .description(app.getDescription())
            .id(app.getId())
            .version(app.getVersion())
            .whenArchived(toOff(app.getWhenArchived()))
            .portfolioId(app.getPortfolio().getId());

    if (opts.contains(FillOpts.Environments)) {

      application.setEnvironments(
          new QDbEnvironment()
              .whenArchived.isNull().parentApplication.eq(app).findList().stream()
                  .map(env -> toEnvironment(env, opts))
                  .collect(Collectors.toList()));

      Map<UUID, UUID> envIds =
        application.getEnvironments().stream().collect(Collectors.toMap(Environment::getId,
          Environment::getId));

      // TODO: Remove in 1.6.0
      application.getEnvironments().stream().forEach(e -> {
        if (!envIds.containsKey(e.getPriorEnvironmentId())) {
          e.setPriorEnvironmentId(null);
        }
      });
    }

    if (opts.contains(FillOpts.Features)) {
      application.setFeatures(
          new QDbApplicationFeature()
              .whenArchived.isNull().parentApplication.eq(app).findList().stream()
                  .map(af -> toApplicationFeature(af, opts))
                  .collect(Collectors.toList()));
    }

    return application;
  }

  @Override
  public Feature toApplicationFeature(DbApplicationFeature af, Opts opts) {
    final Feature feat = new Feature()
      .key(stripArchived(af.getKey(), af.getWhenArchived()))
      .name(af.getName())
      .alias(af.getAlias())
      .link(af.getLink())
      .version(af.getVersion())
      .secret(af.isSecret())
      .whenArchived(toOff(af.getWhenArchived()))
      .valueType(af.getValueType())
      .description(af.getDescription())
      .id(af.getId());

    if (opts.contains(FillOpts.MetaData)) {
      feat.metaData(af.getMetaData());
    }

    return feat;
  }

  @Override
  public Feature toFeature(DbFeatureValue fs) {
    if (fs == null) {
      return null;
    }

    final DbApplicationFeature f = fs.getFeature();

    return new Feature()
        .alias(f.getAlias())
        .id(f.getId())
        .key(stripArchived(f.getKey(), f.getWhenArchived()))
        .link(f.getLink())
        .name(f.getName())
        .secret(f.isSecret())
        .valueType(f.getValueType())
        .version(f.getVersion());
  }

  protected FeatureValue featureValue(
    @Nullable DbApplicationFeature actFeature, @Nullable DbFeatureValue fs, @NotNull Opts opts) {
    if (fs == null) {
      return null;
    }

    final DbApplicationFeature appFeature = actFeature == null ? fs.getFeature() : actFeature;

    final FeatureValue featureValue =
        new FeatureValue()
            .key(stripArchived(appFeature.getKey(), appFeature.getWhenArchived()))
            .locked(fs.isLocked())
            .id(fs.getId())
            .retired(fs.getRetired())
            .version(fs.getVersion());

    if (appFeature.getValueType() == FeatureValueType.BOOLEAN) {
      featureValue.valueBoolean(
          fs.getDefaultValue() == null
              ? Boolean.FALSE
              : Boolean.parseBoolean(fs.getDefaultValue()));
    }
    if (appFeature.getValueType() == FeatureValueType.JSON) {
      featureValue.valueJson(fs.getDefaultValue());
    }
    if (appFeature.getValueType() == FeatureValueType.STRING) {
      featureValue.valueString(fs.getDefaultValue());
    }
    if (appFeature.getValueType() == FeatureValueType.NUMBER) {
      featureValue.valueNumber(
          fs.getDefaultValue() == null ? null : new BigDecimal(fs.getDefaultValue()));
    }

    featureValue.setEnvironmentId(fs.getEnvironment().getId());

    if (opts.contains(FillOpts.RolloutStrategies)) {
      featureValue.setRolloutStrategies(fs.getRolloutStrategies());
      featureValue.setRolloutStrategyInstances(
          fs.getSharedRolloutStrategies().stream()
              .map(
                  srs -> {
                    final DbRolloutStrategy rolloutStrategy = srs.getRolloutStrategy();
                    return new RolloutStrategyInstance()
                        .value(
                            sharedRolloutStrategyToObject(
                                srs.getValue(), appFeature.getValueType()))
                        .name(rolloutStrategy.getName())
                        .disabled(srs.isEnabled() ? null : true)
                        .strategyId(rolloutStrategy.getId());
                  })
              .collect(Collectors.toList()));
    }

    // this is an indicator it is for the UI not for the cache.
    if (opts.contains(FillOpts.People)) {
      featureValue.setWhenUpdated(toOff(fs.getWhenUpdated()));
      featureValue.setWhoUpdated(fs.getWhoUpdated() == null ? null : toPerson(fs.getWhoUpdated()));
    }

    return featureValue;
  }

  private Object sharedRolloutStrategyToObject(String value, FeatureValueType valueType) {
    switch (valueType) {
      case BOOLEAN:
        return Boolean.parseBoolean(value);
      case STRING:
      case JSON:
        return value;
      case NUMBER:
        return new BigDecimal(value);
    }

    return value;
  }

  @Override
  public FeatureValue toFeatureValue(DbFeatureValue fs, Opts opts) {
    return featureValue(null, fs, opts);
  }

  @Override
  public FeatureValue toFeatureValue(@Nullable DbFeatureValue fs) {
    return featureValue(null, fs, Opts.opts(FillOpts.People, FillOpts.RolloutStrategies));
  }

  @Override
  public FeatureValue toFeatureValue(DbApplicationFeature feature, DbFeatureValue value) {
    return featureValue(feature, value, Opts.opts(FillOpts.People));
  }

  @Override
  public FeatureValue toFeatureValue(
      DbApplicationFeature feature, DbFeatureValue value, Opts opts) {
    if (value == null) {
      return new FeatureValue()
          .id(feature.getId())
          .key(stripArchived(feature.getKey(), feature.getWhenArchived()))
          .version(0L)
          .locked(false);
    }

    return featureValue(feature, value, opts);
  }

  @Override
  public RolloutStrategyInfo toRolloutStrategy(DbRolloutStrategy rs, Opts opts) {
    if (rs == null) {
      return null;
    }

    RolloutStrategyInfo info =
        new RolloutStrategyInfo().rolloutStrategy(rs.getStrategy().id(rs.getId().toString()));

    if (opts.contains(FillOpts.SimplePeople)) {
      info.changedBy(toPerson(rs.getWhoChanged()));
    }

    return info;
  }

  @Override
  public DbRolloutStrategy byStrategy(UUID id) {
    return id == null ? null : new QDbRolloutStrategy().id.eq(id).findOne();
  }

  @Override
  public Portfolio toPortfolio(DbPortfolio p, Opts opts) {
    if (p == null) {
      return null;
    }

    Portfolio portfolio =
        new Portfolio()
            .name(stripArchived(p.getName(), p.getWhenArchived()))
            .description(p.getDescription())
            .version(p.getVersion())
            .organizationId(p.getOrganization().getId())
            .id(p.getId());

    if (opts.contains(FillOpts.Portfolios)) {
      portfolio
          .whenCreated(toOff(p.getWhenCreated()))
          .whenUpdated(toOff(p.getWhenUpdated()))
          .createdBy(toPerson(p.getWhoCreated(), p.getOrganization(), Opts.empty()));
    }

    if (opts.contains(FillOpts.Groups)) {
      portfolio.setGroups(
          new QDbGroup()
              .whenArchived.isNull().owningPortfolio.eq(p).order().name.asc().findList().stream()
                  .map(g -> toGroup(g, opts))
                  .collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Applications)) {
      portfolio.setApplications(
          new QDbApplication()
              .whenArchived.isNull().portfolio.eq(p).order().name.asc().findList().stream()
                  .map(a -> toApplication(a, opts))
                  .collect(Collectors.toList()));
    }

    return portfolio;
  }

  @Override
  public Organization toOrganization(DbOrganization org, Opts opts) {
    if (org == null) {
      return null;
    }

    final Organization organisation =
        new Organization()
            .name(stripArchived(org.getName(), org.getWhenArchived()))
            .id(org.getId())
            .whenArchived(toOff(org.getWhenArchived()))
            .admin(true);

    if (opts.contains(FillOpts.Groups)) {
      organisation.orgGroup(
          toGroup(
              new QDbGroup()
                  .adminGroup
                  .isTrue()
                  .owningPortfolio
                  .isNull()
                  .owningOrganization
                  .eq(org)
                  .findOne(),
              Opts.empty()));
    }

    return organisation;
  }

  @Override
  public DbGroup byGroup(UUID gid, Opts opts) {
    if (gid == null) {
      return null;
    }

    QDbGroup eq = new QDbGroup().id.eq(gid);
    if (opts.contains(FillOpts.Members)) {
      eq = eq.groupMembers.person.fetch();
    }

    return eq.findOne();
  }

  @Override
  public DbPerson byPerson(Person creator) {
    if (creator == null || creator.getId() == null || creator.getId().getId() == null) {
      return null;
    }

    return byPerson(creator.getId().getId());
  }

  /** is this person a superuser or portfolio admin for this application */
  @Override
  public boolean isPersonApplicationAdmin(DbPerson dbPerson, DbApplication app) {
    // if a person is in a null portfolio group or portfolio group
    return new QDbGroup()
            .groupMembers.person
            .eq(dbPerson)
            .owningOrganization
            .eq(app.getPortfolio().getOrganization())
            .adminGroup
            .isTrue()
            .or()
            .owningPortfolio
            .isNull()
            .owningPortfolio
            .eq(app.getPortfolio())
            .endOr()
            .exists();
  }

  @Override
  public ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts) {
    return toServiceAccount(sa, opts, null);
  }

  @Override
  public ServiceAccount toServiceAccount(
      DbServiceAccount sa, Opts opts, List<DbAcl> environmentsUserHasAccessTo) {
    if (sa == null) {
      return null;
    }

    ServiceAccount account =
        new ServiceAccount()
            .id(sa.getId())
            .version(sa.getVersion())
            .whenArchived(toOff(sa.getWhenArchived()))
            .portfolioId(sa.getPortfolio().getId())
            .name(sa.getName())
            .description(sa.getDescription());

    if (opts != null) {
      if (!opts.contains(FillOpts.ServiceAccountPermissionFilter)) {
        account.apiKeyServerSide(sa.getApiKeyServerEval());
        account.apiKeyClientSide(sa.getApiKeyClientEval());
      }

      if (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL)) {
        // envId, acl
        Map<UUID, Set<RoleType>> envs = new HashMap<>();

        // we need to figure out what kinds of roles this person has in each environment
        // so that they can't see an SDK URL that has more permissions than they do
        if (environmentsUserHasAccessTo != null) {
          environmentsUserHasAccessTo.forEach(
              acl -> {
                Set<RoleType> e = envs.get(acl.getEnvironment().getId());
                if (e != null) {
                  e.addAll(includeImplicitRead(splitEnvironmentRoles(acl.getRoles())));
                } else {
                  envs.put(
                      acl.getEnvironment().getId(),
                      new HashSet<>(includeImplicitRead(splitEnvironmentRoles(acl.getRoles()))));
                }
              });
        }

        UUID appIdFilter = opts.id(FilterOptType.Application);
        QDbServiceAccountEnvironment permQuery =
            new QDbServiceAccountEnvironment()
              .serviceAccount.eq(sa)
              .environment.whenArchived.isNull()
              .environment.whenUnpublished.isNull();
        if (appIdFilter != null) {
          permQuery = permQuery.environment.parentApplication.id.eq(appIdFilter);
        }

        account.setPermissions(
            permQuery.findList().stream()
                .map(
                    sae ->
                        toServiceAccountPermission(
                            sae,
                            envs.get(sae.getEnvironment().getId()),
                            !envs.isEmpty(),
                            opts.minus(FillOpts.ServiceAccounts)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
      }
    }

    return account;
  }

  private List<RoleType> includeImplicitRead(List<RoleType> splitEnvironmentRoles) {
    List<RoleType> roles = new ArrayList<>(splitEnvironmentRoles);
    if (!splitEnvironmentRoles.isEmpty() && !splitEnvironmentRoles.contains(RoleType.READ)) {
      roles.add(RoleType.READ);
    }
    return roles;
  }

  @Override
  public FeatureEnvironment toFeatureEnvironment(
    DbFeatureValue featureValue, @NotNull List<RoleType> roles, @NotNull DbEnvironment dbEnvironment, @NotNull Opts opts) {
    final FeatureEnvironment featureEnvironment =
        new FeatureEnvironment()
            .environment(toEnvironment(dbEnvironment, Opts.empty()))
            .roles(roles)
            .featureValue(toFeatureValue(featureValue));

    if (opts.contains(FillOpts.ServiceAccounts)) {
      featureEnvironment.serviceAccounts(
          dbEnvironment.getServiceAccountEnvironments().stream()
              .filter(
                  sae ->
                      opts.contains(FillOpts.Archived)
                          || sae.getServiceAccount().getWhenArchived() == null)
              .map((sae) -> toServiceAccount(sae.getServiceAccount(), null, null))
              .sorted(
                  Comparator.comparing(
                      ServiceAccount
                          ::getId)) // this is really only because the test is finicky, it should be
                                    // removed
              .collect(Collectors.toList()));
    }

    return featureEnvironment;
  }

  public Group getSuperuserGroup(Opts opts) {
    final DbGroup g =
        new QDbGroup()
            .owningOrganization
            .id.eq(getOrganizationId())
            .adminGroup
            .isTrue()
            .owningPortfolio
            .isNull()
            .groupMembers.person
            .fetch()
            .findOne();

    if (g
        != null) { // make sure you are a user in at least one group otherwise you can't see this
                   // group
      return toGroup(g, opts);
    }

    return null;
  }
}
