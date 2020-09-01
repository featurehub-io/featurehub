package io.featurehub.db.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbAcl;
import io.featurehub.db.model.DbApplication;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.DbPortfolio;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.DbServiceAccountEnvironment;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.ApplicationGroupRole;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.EnvironmentGroupRole;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.HiddenEnvironments;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.RoleType;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.RolloutStrategyInfo;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface Conversions {
  static Optional<UUID> uuid(String id) {
    if (id == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(UUID.fromString(id));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  static UUID ifUuid(String id) {
    if (id == null) {
      return null;
    }
    try {
      return UUID.fromString(id);
    } catch(Exception e) {
      return null;
    }
  }

  static <T> T readJsonValue(String content, Class<T> valueType)  {
    try {
      return CacheJsonMapper.mapper.readValue(content, valueType);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  static String valueToJsonString(HiddenEnvironments environments) {
    try {
      return CacheJsonMapper.mapper.writeValueAsString(environments);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  // used so things that call toPerson multiple times can hold onto it
  DbOrganization getDbOrganization();

  DbPerson uuidPerson(String id);

  DbPerson uuidPerson(String id, Opts opts);

  DbPortfolio uuidPortfolio(String id);

  DbEnvironment uuidEnvironment(String id);

  DbEnvironment uuidEnvironment(String id, Opts opts);

  DbApplication uuidApplication(String id);

  boolean personIsNotSuperAdmin(DbPerson person);
  boolean personIsSuperAdmin(DbPerson person);
  Group getSuperuserGroup(Opts opts);

  String limitLength(String s, int len);

  Environment toEnvironment(DbEnvironment env, Opts opts, Set<DbApplicationFeature> features);

  Environment toEnvironment(DbEnvironment env, Opts opts);

  String getCacheNameByEnvironment(DbEnvironment env);

  ServiceAccountPermission toServiceAccountPermission(DbServiceAccountEnvironment sae, Set<RoleType> rolePerms, boolean mustHaveRolePerms, Opts opt);

  ApplicationGroupRole applicationGroupRoleFromAcl(DbAcl acl);

  EnvironmentGroupRole environmentGroupRoleFromAcl(DbAcl acl);

  List<RoleType> splitEnvironmentRoles(String roles);

  List<ApplicationRoleType> splitApplicationRoles(String roles);

  EnvironmentGroupRole convertEnvironmentAcl(DbAcl dbAcl);

  OffsetDateTime toOff(LocalDateTime ldt);

  Person toPerson(DbPerson person);

  Person toPerson(DbPerson dbp, Opts opts);
  Person toPerson(DbPerson dbp, DbOrganization org, Opts opts);

  default String stripArchived(String name, LocalDateTime whenArchived) {
    if (whenArchived == null) {
      return name;
    }

    int prefix = name.indexOf(DbArchiveStrategy.archivePrefix);
    if (prefix == -1) {
      return name;
    }

    return name.substring(0, prefix);
  }

  Group toGroup(DbGroup dbg, Opts opts);

  Application toApplication(DbApplication app, Opts opts);

  Feature toApplicationFeature(DbApplicationFeature af, Opts opts);

  Feature toFeature(DbFeatureValue fs);

  FeatureValue toFeatureValue(DbFeatureValue fs);
  FeatureValue toFeatureValue(DbFeatureValue fs, Opts opts);

  Portfolio toPortfolio(DbPortfolio p, Opts opts);

  Organization toOrganization(DbOrganization org, Opts opts);

  DbGroup uuidGroup(String gid, Opts opts);

  DbPerson uuidPerson(Person creator);

  boolean isPersonApplicationAdmin(DbPerson dbPerson, DbApplication app);

  ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts);

  ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts, List<DbAcl> environmentsUserHasAccessTo);

  default List<RoleType> splitServiceAccountPermissions(String permissions) {
    // same now, were different historically
    return splitEnvironmentRoles(permissions);
  }

  FeatureEnvironment toFeatureEnvironment(DbFeatureValue s, List<RoleType> roles, DbEnvironment dbEnvironment, Opts opts);

  FeatureValue toFeatureValue(DbApplicationFeature feature, DbFeatureValue value);
  FeatureValue toFeatureValue(DbApplicationFeature feature, DbFeatureValue value, Opts opts);

  RolloutStrategyInfo toRolloutStrategy(DbRolloutStrategy rs, Opts opts);

  DbRolloutStrategy uuidStrategy(String id);

}
