package io.featurehub.db.services;

import io.ebean.Database;
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
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbGroup;
import io.featurehub.db.model.query.QDbNamedCache;
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
import io.featurehub.mr.model.ServiceAccountPermissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class ConvertUtils {
  private static final Logger log = LoggerFactory.getLogger(ConvertUtils.class);
  private final Database database;

  @Inject
  public ConvertUtils(Database database) {
    this.database = database;
  }

  public static Optional<UUID> uuid(String id) {
    if (id == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(UUID.fromString(id));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public static UUID ifUuid(String id) {
    if (id == null) {
      return null;
    }
    try {
      return UUID.fromString(id);
    } catch(Exception e) {
      return null;
    }
  }

  public DbPerson uuidPerson(String id) {
    return uuid(id).map(personId -> new QDbPerson().id.eq(personId).findOne()).orElse(null);
  }

  public DbPerson uuidPerson(String id, Opts opts) {
    return uuid(id).map(personId -> {
      QDbPerson finder = new QDbPerson().id.eq(personId);
      if (opts.contains(FillOpts.Groups)) {
        finder = finder.groupsPersonIn.fetch();
      }
      return finder.findOne();
    }).orElse(null);
  }


  public DbPortfolio uuidPortfolio(String id) {
    return uuid(id).map(pId -> new QDbPortfolio().id.eq(pId).findOne()).orElse(null);
  }

  public DbEnvironment uuidEnvironment(String id) {
    if (id == null) {
      return null;
    }

    return uuid(id).map(eId -> new QDbEnvironment().id.eq(eId).findOne()).orElse(null);
  }

  public DbApplication uuidApplication(String id) {
    return uuid(id).map(aId -> new QDbApplication().id.eq(aId).findOne()).orElse(null);
  }

  public boolean personIsNotSuperAdmin(DbPerson person) {
    return new QDbGroup().and().owningPortfolio.isNull().peopleInGroup.id.eq(person.getId()).endAnd().findCount() <= 0;
  }

  public String limitLength(String s, int len) {
    return s == null ? null : (s.length() > len ? s.substring(0, len) : s);
  }

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
      environment.updatedBy(toPerson(env.getWhoCreated(), Opts.empty()));
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
          .map(sae -> toServiceAccountPermission(sae, opts)).collect(Collectors.toList()));
    }

    // collect all of the ACls for all of the groups for this environment?
    if (opts.contains(FillOpts.Acls)) {
      new QDbAcl().environment.eq(env).findEach(acl -> {
        environment.addGroupRolesItem(environmentGroupRoleFromAcl(acl));
      });
    }

    return environment;
  }

  public Environment toEnvironment(DbEnvironment env, Opts opts) {
    return toEnvironment(env, opts, null);
  }

  public String getCacheNameByEnvironment(DbEnvironment env) {
    return new QDbNamedCache().organizations.portfolios.applications.environments.eq(env).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);
  }

  public String getCacheNameByEnvironmentId(String eid) {
    UUID id = ConvertUtils.ifUuid(eid);
    if (id == null) {
      return null;
    }

    return new QDbNamedCache().organizations.portfolios.applications.environments.id.eq(id).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);
  }

  public ServiceAccountPermission toServiceAccountPermission(DbServiceAccountEnvironment sae, Opts opt) {
    final ServiceAccountPermission sap = new ServiceAccountPermission()
      .id(sae.getId().toString())
      .permissions(splitServiceAccountPermissions(sae.getPermissions()))
      .environmentId(sae.getEnvironment().getId().toString());

    if (opt.contains(FillOpts.ServiceAccounts) || opt.contains(FillOpts.SdkURL)) {
      sap.serviceAccount(toServiceAccount(sae.getServiceAccount(), opt.minus(FillOpts.Permissions)));
    }

    if (opt.contains(FillOpts.SdkURL)) {
      String cacheName = getCacheNameByEnvironment(sae.getEnvironment());
      sap.sdkUrl(String.format("%s/%s/%s", cacheName, sap.getEnvironmentId(), sap.getServiceAccount().getApiKey()));
    }

    return sap;
  }

  public ApplicationGroupRole applicationGroupRoleFromAcl(DbAcl acl) {
    return new ApplicationGroupRole()
      .groupId(acl.getGroup().getId().toString())
      .roles(splitApplicationRoles(acl.getRoles()))
      .applicationId(acl.getApplication().getId().toString());
  }

  public EnvironmentGroupRole environmentGroupRoleFromAcl(DbAcl acl) {
    return new EnvironmentGroupRole()
      .groupId(acl.getGroup().getId().toString())
      .roles(splitEnvironmentRoles(acl.getRoles()))
      .environmentId(acl.getEnvironment().getId().toString());
  }

  public List<RoleType> splitEnvironmentRoles(String roles) {
    if (roles == null || roles.length() == 0) {
      return null;
    }

    List<RoleType> roleTypes = new ArrayList<>();

    if (roleTypes != null) {
      for(String n : roles.split(",")) {
        try {
          roleTypes.add(RoleType.valueOf(n));
        } catch (Exception e) { return null; }
      }
    }

    return roleTypes;
  }

  private List<ApplicationRoleType> splitApplicationRoles(String roles) {
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

  public EnvironmentGroupRole convertEnvironmentAcl(DbAcl dbAcl) {
    return new EnvironmentGroupRole().environmentId(dbAcl.getEnvironment().getId().toString())
      .groupId(dbAcl.getGroup().getId().toString());
  }

  public OffsetDateTime toOff(LocalDateTime ldt) {
    return  ldt == null ? null : ldt.atOffset(ZoneOffset.UTC);
  }

  public Person toPerson(DbPerson person) {
    if (person == null) {
      return null;
    }

    return new Person().id(new PersonId().id(person.getId().toString()))
      .version(person.getVersion())
      .passwordRequiresReset(person.isPasswordRequiresReset())
      .email(person.getEmail()).name(person.getName());
  }

  public Person toPerson(DbPerson dbp, Opts opts) {
    if (dbp == null) {
      return null;
    }

    Person p = new Person()
      .email(dbp.getEmail())
      .name(stripArchived(dbp.getName(), dbp.getWhenArchived()))
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
      dbp.getGroupsPersonIn().forEach(dbg -> {
        p.addGroupsItem(toGroup(dbg, opts.minus(FillOpts.Groups)));
      });
    }

    return p;
  }

  private String stripArchived(String name, LocalDateTime whenArchived) {
    if (whenArchived == null) {
      return name;
    }

    int prefix = name.indexOf(DbArchiveStrategy.archivePrefix);
    if (prefix == -1) {
      return name;
    }

    return name.substring(0, prefix);
  }

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
      group.setOrganizationId(dbg.getOwningPortfolio().getOrganization().getId().toString());
    } else {
      group.setOrganizationId(dbg.getOwningOrganization() == null ? null : dbg.getOwningOrganization().getId().toString());
    }

    if (opts.contains(FillOpts.Members)) {
      group.setMembers(dbg.getPeopleInGroup().stream().map(p -> this.toPerson(p, opts.minus(FillOpts.Members, FillOpts.Acls))).collect(Collectors.toList()));
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
      application.setEnvironments(app.getEnvironments().stream().map(env -> toEnvironment(env, opts)).collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Features)) {
      application.setFeatures(app.getFeatures().stream().map(af -> toApplicationFeature(af, opts)).collect(Collectors.toList()));
    }

    return application;
  }

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
      featureValue.valueBoolean(fs.getDefaultValue() == null ? null : Boolean.parseBoolean(fs.getDefaultValue()));
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
        .createdBy(toPerson(p.getWhoCreated(), Opts.empty()));
    }

    if (opts.contains(FillOpts.Groups)) {
      portfolio.setGroups(p.getGroups()
        .stream()
        .map(g -> toGroup(g, opts))
        .collect(Collectors.toList()));
    }

    if (opts.contains(FillOpts.Applications)) {
      portfolio.setApplications(p.getApplications()
        .stream()
        .map(a -> toApplication(a, opts))
        .collect(Collectors.toList()));
    }

    return portfolio;
  }

  public Organization toOrganization(DbOrganization org) {
    if (org == null) {
      return null;
    }

    return new Organization()
      .name(stripArchived(org.getName(), org.getWhenArchived()))
      .id(org.getId().toString())
      .whenArchived(toOff(org.getWhenArchived()))
      .orgGroup(toGroup(new QDbGroup().adminGroup.isTrue().owningPortfolio.isNull().owningOrganization.eq(org).findOne(), Opts.empty()))
      .admin(true);
  }

  public DbGroup uuidGroup(String gid, Opts opts) {
    return uuid(gid).map(g -> {
      QDbGroup eq = new QDbGroup().id.eq(g);
      if (opts.contains(FillOpts.Members)) {
        eq = eq.peopleInGroup.fetch();
      }
      return eq.findOne();
    }).orElse(null);
  }

  public DbPerson uuidPerson(Person creator) {
    if (creator == null || creator.getId() == null || creator.getId().getId() == null ) {
      return null;
    }

    return uuidPerson(creator.getId().getId());
  }

  public ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts) {
    if (sa == null) {
      return null;
    }

    ServiceAccount account = new ServiceAccount()
      .id(sa.getId().toString())
      .version(sa.getVersion())
      .whenArchived(toOff(sa.getWhenArchived()))
      .name(sa.getName()).description(sa.getDescription());

    if (opts != null) {
      account.apiKey(sa.getApiKey());

      if (opts.contains(FillOpts.Permissions)) {
        account.setPermissions(
          sa.getServiceAccountEnvironments().stream()
            .map(sae -> toServiceAccountPermission(sae, opts.minus(FillOpts.ServiceAccounts, FillOpts.SdkURL)))
            .collect(Collectors.toList()));
      }
    }

    return account;
  }

  private List<ServiceAccountPermissionType> splitServiceAccountPermissions(String permissions) {
    return Arrays.stream(permissions.split(","))
      .filter(s -> s != null && s.length() > 0)
      .map(ServiceAccountPermissionType::fromValue)
      .collect(Collectors.toList());
  }

  public FeatureEnvironment toFeatureEnvironment(DbEnvironmentFeatureStrategy s, List<RoleType> roles, DbEnvironment dbEnvironment, Opts opts) {
    final FeatureEnvironment featureEnvironment = new FeatureEnvironment()
      .environment(toEnvironment(dbEnvironment, Opts.empty()))
      .roles(roles)
      .featureValue(toFeatureValue(s));

    if (opts.contains(FillOpts.ServiceAccounts)) {
      featureEnvironment.serviceAccounts(dbEnvironment.getServiceAccountEnvironments().stream()
        .filter(sae -> opts.contains(FillOpts.Archived) || sae.getServiceAccount().getWhenArchived() == null)
        .map((sae) -> toServiceAccount(sae.getServiceAccount(), null)).collect(Collectors.toList()));
    }

    return featureEnvironment;
  }

  public FeatureValue toFeatureValue(DbApplicationFeature feature, DbEnvironmentFeatureStrategy value) {
    if (value == null) {
      return new FeatureValue().id(feature.getId().toString()).key(stripArchived(feature.getKey(), feature.getWhenArchived())).locked(false);
    }

    return toFeatureValue(value);
  }
}
