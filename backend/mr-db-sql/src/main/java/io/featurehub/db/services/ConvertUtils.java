package io.featurehub.db.services;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbEnvironmentFeatureStrategy;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbNamedCache;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.DbServiceAccountEnvironment;
import io.featurehub.db.model.query.QDbAcl;
import io.featurehub.db.model.query.QDbApplication;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.model.query.QDbPortfolio;
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
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;

import javax.inject.Singleton;
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

  @Override
  public DbPerson uuidPerson(String id) {
    return Conversions.uuid(id).map(personId -> new QDbPerson().id.eq(personId).findOne()).orElse(null);
  }

  @Override
  public DbPerson uuidPerson(String id, Opts opts) {
    return Conversions.uuid(id).map(personId -> {
      QDbPerson finder = new QDbPerson().id.eq(personId);
      if (opts.contains(FillOpts.Groups)) {
        finder = finder.groupsPersonIn.fetch();
      }
      return finder.findOne();
    }).orElse(null);
  }


  @Override
  public DbPortfolio uuidPortfolio(String id) {
    return Conversions.uuid(id).map(pId -> new QDbPortfolio().id.eq(pId).findOne()).orElse(null);
  }

  @Override
  public DbEnvironment uuidEnvironment(String id) {
    if (id == null) {
      return null;
    }

    return Conversions.uuid(id).map(eId -> new QDbEnvironment().id.eq(eId).findOne()).orElse(null);
  }

  @Override
  public DbEnvironment uuidEnvironment(String id, Opts opts) {
    if (id == null) {
      return null;
    }

    return Conversions.uuid(id).map(eId -> {
      final QDbEnvironment eq = new QDbEnvironment().id.eq(eId);
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
    }).orElse(null);
  }


  @Override
  public DbApplication uuidApplication(String id) {
    return Conversions.uuid(id).map(aId -> new QDbApplication().id.eq(aId).findOne()).orElse(null);
  }

  @Override
  public boolean personIsNotSuperAdmin(DbPerson person) {
    return new QDbGroup().owningPortfolio.isNull().adminGroup.isTrue().peopleInGroup.id.eq(person.getId()).findCount() <= 0;
  }

  @Override
  public boolean personIsSuperAdmin(DbPerson person) {
    return new QDbGroup()
      .whenArchived.isNull()
      .owningPortfolio.isNull()
      .peopleInGroup.eq(person)
      .adminGroup.isTrue().findCount() > 0;
  }

  @Override
  public String limitLength(String s, int len) {
    return s == null ? null : (s.length() > len ? s.substring(0, len) : s);
  }

  @Override
  public Environment toEnvironment(DbEnvironment env, Opts opts, Set<DbApplicationFeature> features) {
    if (env == null) {
      return null;
    }

    Environment environment = new Environment()
      .id(env.getId().toString())
      .name(stripArchived(env.getName(), env.getWhenArchived()))
      .version(env.getVersion())
      .production(env.isProductionEnvironment())
      .priorEnvironmentId(env.getPriorEnvironment() != null ? env.getPriorEnvironment().getId().toString() : null)
      .applicationId(env.getParentApplication().getId().toString());

    if (opts.contains(FillOpts.People)) {
      environment.updatedBy(toPerson(env.getWhoCreated(),
        env.getParentApplication().getPortfolio().getOrganization()
        , Opts.empty()));
      environment.createdBy(toPerson(env.getWhoCreated()));
    }

    if (opts.contains(FillOpts.Features)) {
      if (features != null) {
        environment.setFeatures(features.stream().map(ef -> toApplicationFeature(ef, Opts.empty())).collect(Collectors.toList()));
      } else {
        environment.setFeatures(env.getEnvironmentFeatures()
          .stream()
          .filter(f -> opts.contains(FillOpts.Archived) || f.getFeature().getWhenArchived() == null)
          .map(ef -> toApplicationFeature(ef.getFeature(), Opts.empty())).collect(Collectors.toList()));
      }
    }

    if (opts.contains(FillOpts.ServiceAccounts) || opts.contains(FillOpts.SdkURL)) {
      environment.setServiceAccountPermission(
        env.getServiceAccountEnvironments().stream()
          .filter(sae -> opts.contains(FillOpts.Archived) || sae.getServiceAccount().getWhenArchived() == null)
          .map(sae -> toServiceAccountPermission(sae, null, false, opts)).collect(Collectors.toList()));
    }

    // collect all of the ACls for all of the groups for this environment?
    if (opts.contains(FillOpts.Acls)) {
      new QDbAcl().environment.eq(env).findEach(acl -> {
        environment.addGroupRolesItem(environmentGroupRoleFromAcl(acl));
      });
    }

    return environment;
  }

  @Override
  public Environment toEnvironment(DbEnvironment env, Opts opts) {
    return toEnvironment(env, opts, null);
  }

  @Override
  public String getCacheNameByEnvironment(DbEnvironment env) {
    return new QDbNamedCache().organizations.portfolios.applications.environments.eq(env).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);
  }

  @Override
  public ServiceAccountPermission toServiceAccountPermission(DbServiceAccountEnvironment sae, Set<RoleType> rolePerms, boolean mustHaveRolePerms, Opts opt) {
    final ServiceAccountPermission sap = new ServiceAccountPermission()
      .id(sae.getId().toString())
      .permissions(splitServiceAccountPermissions(sae.getPermissions()))
      .environmentId(sae.getEnvironment().getId().toString());

    if (sap.getPermissions().isEmpty() && opt.contains(FillOpts.IgnoreEmptyPermissions)) {
      return null;
    }

    // if they don't have read, but they really do have read, add read
    if (!sap.getPermissions().isEmpty() && !sap.getPermissions().contains(RoleType.READ)) {
      sap.getPermissions().add(RoleType.READ);
    }

    if (opt.contains(FillOpts.ServiceAccounts) || opt.contains(FillOpts.SdkURL)) {
      sap.serviceAccount(toServiceAccount(sae.getServiceAccount(), opt.minus(FillOpts.Permissions, FillOpts.SdkURL)));
    }

    if (opt.contains(FillOpts.SdkURL)) {
      // if role perms is null (i.e we don't care) or the roles that a person has is a super-set of the roles of the service account
      if (!mustHaveRolePerms || (rolePerms != null && rolePerms.containsAll(sap.getPermissions()))) {
        String cacheName = getCacheNameByEnvironment(sae.getEnvironment());
        sap.sdkUrl(String.format("%s/%s/%s", cacheName, sap.getEnvironmentId(), sap.getServiceAccount().getApiKey()));
      }
    }

    return sap;
  }

  @Override
  public ApplicationGroupRole applicationGroupRoleFromAcl(DbAcl acl) {
    return new ApplicationGroupRole()
      .groupId(acl.getGroup().getId().toString())
      .roles(splitApplicationRoles(acl.getRoles()))
      .applicationId(acl.getApplication().getId().toString());
  }

  @Override
  public EnvironmentGroupRole environmentGroupRoleFromAcl(DbAcl acl) {
    return new EnvironmentGroupRole()
      .groupId(acl.getGroup().getId().toString())
      .roles(splitEnvironmentRoles(acl.getRoles()))
      .environmentId(acl.getEnvironment().getId().toString());
  }

  @Override
  public List<RoleType> splitEnvironmentRoles(String roles) {
    List<RoleType> roleTypes = new ArrayList<>();
    if (roles == null || roles.length() == 0) {
      return roleTypes;
    }

    for(String n : roles.split(",")) {
      try {
        roleTypes.add(RoleType.valueOf(n));
      } catch (Exception e) { return null; }
    }

    return roleTypes;
  }

  @Override
  public List<ApplicationRoleType> splitApplicationRoles(String roles) {
    List<ApplicationRoleType> roleTypes = new ArrayList<>();

    if (roles != null) {
      for(String n : roles.split(",")) {
        try {
          roleTypes.add(ApplicationRoleType.valueOf(n));
        } catch (Exception e) { return null; }
      }
    }

    return roleTypes;
  }

  @Override
  public EnvironmentGroupRole convertEnvironmentAcl(DbAcl dbAcl) {
    return new EnvironmentGroupRole().environmentId(dbAcl.getEnvironment().getId().toString())
      .groupId(dbAcl.getGroup().getId().toString());
  }

  @Override
  public OffsetDateTime toOff(LocalDateTime ldt) {
    return  ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
  }

  @Override
  public Person toPerson(DbPerson person) {
    if (person == null) {
      return null;
    }

    return new Person().id(new PersonId().id(person.getId().toString()))
      .version(person.getVersion())
      .passwordRequiresReset(person.isPasswordRequiresReset())
      .email(person.getEmail()).name(person.getName())
      .groups(null);
  }

  public DbOrganization getDbOrganization() {
    return new QDbOrganization().findOne();
  }

  @Override
  public Person toPerson(DbPerson dbp, Opts opts) {
    return toPerson(dbp, null, opts);
  }

  @Override
  public Person toPerson(DbPerson dbp, DbOrganization org, Opts opts) {
    if (dbp == null) {
      return null;
    }

    Person p = new Person()
      .email(dbp.getEmail())
      .name(dbp.getName() == null ? "" : stripArchived(dbp.getName(), dbp.getWhenArchived()))
      .version(dbp.getVersion())
      .passwordRequiresReset(dbp.isPasswordRequiresReset())
      .whenArchived(toOff(dbp.getWhenArchived()))
      .id(new PersonId().id(dbp.getId().toString()));

    if (opts.contains(FillOpts.Details)) {
      if (dbp.getWhenUpdated() != null) {
        p.whenUpdated(toOff(dbp.getWhenUpdated()));
      }

      p.whenCreated(toOff(dbp.getWhenCreated()));
    }

    if (opts.contains(FillOpts.Groups)) {
      new QDbGroup()
        .whenArchived.isNull()
        .peopleInGroup.eq(dbp)
        .owningOrganization.eq(org == null ? getDbOrganization() : org)
        .findList().forEach(dbg -> {
        p.addGroupsItem(toGroup(dbg, opts.minus(FillOpts.Groups)));
      });
    }

    return p;
  }

  @Override
  public Group toGroup(DbGroup dbg, Opts opts) {
    if (dbg == null) {
      return null;
    }

    Group group = new Group()
      .version(dbg.getVersion())
      .whenArchived(toOff(dbg.getWhenArchived()))
      ;
    group.setId(dbg.getId().toString());
    group.setName(stripArchived(dbg.getName(), dbg.getWhenArchived()));
    group.setAdmin(dbg.isAdminGroup());
    if (dbg.getOwningPortfolio() != null) {
      group.setPortfolioId(dbg.getOwningPortfolio().getId().toString());
    }
    group.setOrganizationId(dbg.getOwningOrganization() == null ? null : dbg.getOwningOrganization().getId().toString());

    if (opts.contains(FillOpts.Members)) {
      DbOrganization org = dbg.getOwningOrganization() == null ? dbg.getOwningPortfolio().getOrganization() :
        dbg.getOwningOrganization();
      group.setMembers(
        new QDbPerson().order().name.asc().whenArchived.isNull().groupsPersonIn.eq(dbg).findList().stream()
        .map(p -> this.toPerson(p, org, opts.minus(FillOpts.Members, FillOpts.Acls, FillOpts.Groups))).collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Acls)) {
      new QDbAcl().group.eq(dbg)
        .findEach(acl -> {
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

    Application application = new Application()
      .name(stripArchived(app.getName(), app.getWhenArchived()))
      .description(app.getDescription())
      .id(app.getId().toString())
      .version(app.getVersion())
      .whenArchived(toOff(app.getWhenArchived()))
      .portfolioId(app.getPortfolio().getId().toString());

    if (opts.contains(FillOpts.Environments)) {

      application.setEnvironments(new QDbEnvironment()
        .whenArchived.isNull().parentApplication.eq(app)
        .findList().stream().map(env -> toEnvironment(env, opts)).collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Features)) {
      application.setFeatures(
        new QDbApplicationFeature().whenArchived.isNull().parentApplication.eq(app).findList()
          .stream().map(af -> toApplicationFeature(af, opts)).collect(Collectors.toList()));
    }

    return application;
  }

  @Override
  public Feature toApplicationFeature(DbApplicationFeature af, Opts opts) {
    return new Feature()
      .key(stripArchived(af.getKey(), af.getWhenArchived()))
      .name(af.getName())
      .alias(af.getAlias())
      .link(af.getLink())
      .version(af.getVersion())
      .secret(af.isSecret())
      .whenArchived(toOff(af.getWhenArchived()))
      .valueType(af.getValueType())
      .id(af.getId().toString());
  }

  @Override
  public Feature toFeature(DbEnvironmentFeatureStrategy fs) {
    if (fs == null) {
      return null;
    }

    final DbApplicationFeature f = fs.getFeature();

    return new Feature()
      .alias(f.getAlias())
      .id(f.getId().toString())
      .key(stripArchived(f.getKey(), f.getWhenArchived()))
      .link(f.getLink())
      .name(f.getName())
      .secret(f.isSecret())
      .valueType(f.getValueType())
      .version(f.getVersion());
  }

  @Override
  public FeatureValue toFeatureValue(DbEnvironmentFeatureStrategy fs) {
    if (fs == null) {
      return null;
    }

    final FeatureValue featureValue = new FeatureValue()
      .key(stripArchived(fs.getFeature().getKey(), fs.getFeature().getWhenArchived()))
      .rolloutStrategy(fs.getEnabledStrategy())
      .locked(fs.isLocked())
      .id(fs.getId().toString())
      .version(fs.getVersion())
      .whenUpdated(toOff(fs.getWhenUpdated()))
      .rolloutStrategyInstances(fs.getRolloutStrategyInstances());

    final DbApplicationFeature feature = fs.getFeature();
    if (feature.getValueType() == FeatureValueType.BOOLEAN) {
      featureValue.valueBoolean(fs.getDefaultValue() == null ? Boolean.FALSE : Boolean.parseBoolean(fs.getDefaultValue()));
    }
    if (feature.getValueType() == FeatureValueType.JSON) {
      featureValue.valueJson(fs.getDefaultValue());
    }
    if (feature.getValueType() == FeatureValueType.STRING) {
      featureValue.valueString(fs.getDefaultValue());
    }
    if (feature.getValueType() == FeatureValueType.NUMBER) {
      featureValue.valueNumber(fs.getDefaultValue() == null ? null : new BigDecimal(fs.getDefaultValue()));
    }

    featureValue.setEnvironmentId(fs.getEnvironment().getId().toString());
    featureValue.setWhoUpdated(fs.getWhoUpdated() == null ? null : toPerson(fs.getWhoUpdated()));

    return featureValue;
  }

  @Override
  public Portfolio toPortfolio(DbPortfolio p, Opts opts) {
    if (p == null) {
      return null;
    }

    Portfolio portfolio = new Portfolio()
      .name(stripArchived(p.getName(), p.getWhenArchived()))
      .description(p.getDescription())
      .version(p.getVersion())
      .organizationId(p.getOrganization().getId().toString())
      .id(p.getId().toString());

    if (opts.contains(FillOpts.Portfolios)) {
      portfolio
        .whenCreated(toOff(p.getWhenCreated()))
        .whenUpdated(toOff(p.getWhenUpdated()))
        .createdBy(toPerson(p.getWhoCreated(), p.getOrganization(), Opts.empty()));
    }

    if (opts.contains(FillOpts.Groups)) {
      portfolio.setGroups(
        new QDbGroup().whenArchived.isNull().owningPortfolio.eq(p).order().name.asc().findList().stream()
        .map(g -> toGroup(g, opts))
        .collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Applications)) {
      portfolio.setApplications(
          new QDbApplication().whenArchived.isNull().portfolio.eq(p).order().name.asc().findList()
        .stream()
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

    final Organization organisation = new Organization()
      .name(stripArchived(org.getName(), org.getWhenArchived()))
      .id(org.getId().toString())
      .whenArchived(toOff(org.getWhenArchived()))
      .admin(true);

    if (opts.contains(FillOpts.Groups)) {
      organisation.orgGroup(
        toGroup(
          new QDbGroup().adminGroup.isTrue().owningPortfolio.isNull().owningOrganization.eq(org).findOne(), Opts.empty()));

    }

    return organisation;
  }

  @Override
  public DbGroup uuidGroup(String gid, Opts opts) {
    return Conversions.uuid(gid).map(g -> {
      QDbGroup eq = new QDbGroup().id.eq(g);
      if (opts.contains(FillOpts.Members)) {
        eq = eq.peopleInGroup.fetch();
      }
      return eq.findOne();
    }).orElse(null);
  }

  @Override
  public DbPerson uuidPerson(Person creator) {
    if (creator == null || creator.getId() == null || creator.getId().getId() == null ) {
      return null;
    }

    return uuidPerson(creator.getId().getId());
  }

  /**
   * is this person a superuser or portfolio admin for this application
   */
  @Override
  public boolean isPersonApplicationAdmin(DbPerson dbPerson, DbApplication app) {
    DbOrganization org = app.getPortfolio().getOrganization();
    // if a person is in a null portfolio group or portfolio group
    return new QDbGroup()
      .peopleInGroup.eq(dbPerson)
      .owningOrganization.eq(app.getPortfolio().getOrganization())
      .adminGroup.isTrue()
      .or()
        .owningPortfolio.isNull()
        .owningPortfolio.eq(app.getPortfolio())
      .endOr()
      .findCount() > 0;
  }

  @Override
  public ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts) {
    return toServiceAccount(sa, opts, null);
  }

  @Override
  public ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts, List<DbAcl> environmentsUserHasAccessTo) {
    if (sa == null) {
      return null;
    }

    ServiceAccount account = new ServiceAccount()
      .id(sa.getId().toString())
      .version(sa.getVersion())
      .whenArchived(toOff(sa.getWhenArchived()))
      .portfolioId(sa.getPortfolio().getId().toString())
      .name(sa.getName()).description(sa.getDescription());

    if (opts != null) {
      account.apiKey(sa.getApiKey());

      if (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL)) {
        // envId, acl
        Map<UUID, Set<RoleType>> envs = new HashMap<>();

        // we need to figure out what kinds of roles this person has in each environment
        // so that they can't see an SDK URL that has more permissions than they do
        if (environmentsUserHasAccessTo != null) {
          environmentsUserHasAccessTo.forEach(acl -> {
            Set<RoleType> e = envs.get(acl.getEnvironment().getId());
            if (e != null) {
              e.addAll(splitEnvironmentRoles(acl.getRoles()));
            } else {
              envs.put(acl.getEnvironment().getId(), new HashSet<>(splitEnvironmentRoles(acl.getRoles())));
            }
          });
        }

        account.setPermissions(
          sa.getServiceAccountEnvironments().stream()
            .map(sae -> toServiceAccountPermission(sae,
              envs.get(sae.getEnvironment().getId()),
              !envs.isEmpty(),
              opts.minus(FillOpts.ServiceAccounts)))
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
      }
    }

    return account;
  }

  @Override
  public FeatureEnvironment toFeatureEnvironment(DbEnvironmentFeatureStrategy s, List<RoleType> roles, DbEnvironment dbEnvironment, Opts opts) {
    final FeatureEnvironment featureEnvironment = new FeatureEnvironment()
      .environment(toEnvironment(dbEnvironment, Opts.empty()))
      .roles(roles)
      .featureValue(toFeatureValue(s));

    if (opts.contains(FillOpts.ServiceAccounts)) {
      featureEnvironment.serviceAccounts(dbEnvironment.getServiceAccountEnvironments().stream()
        .filter(sae -> opts.contains(FillOpts.Archived) || sae.getServiceAccount().getWhenArchived() == null)
        .map((sae) -> toServiceAccount(sae.getServiceAccount(), null, null))
        .sorted(Comparator.comparing(ServiceAccount::getId)) // this is really only because the test is finicky, it should be removed
        .collect(Collectors.toList()));
    }

    return featureEnvironment;
  }

  @Override
  public FeatureValue toFeatureValue(DbApplicationFeature feature, DbEnvironmentFeatureStrategy value) {
    if (value == null) {
      return new FeatureValue().id(feature.getId().toString()).key(stripArchived(feature.getKey(), feature.getWhenArchived())).locked(false);
    }

    return toFeatureValue(value);
  }
}
