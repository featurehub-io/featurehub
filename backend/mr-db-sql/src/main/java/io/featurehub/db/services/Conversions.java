package io.featurehub.db.services;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.featurehub.jersey.config.CacheJsonMapper;
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
import io.featurehub.mr.model.RolloutStrategyInfo;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountPermission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface Conversions {
  // in case a field can be a name or a uuid (i.e. feature key)
  static UUID checkUuid(String id) {
    if (id == null) {
      return null;
    }
    try {
      return UUID.fromString(id);
    } catch (Exception e) {
      return null;
    }
  }

  static void nonNullPortfolioId(UUID portfolioId) {
    if (portfolioId == null) {
      throw new IllegalArgumentException("Must provide non null portfolioId");
    }
  }

  static void nonNullPerson(Person person) {
    if (person == null || person.getId() == null || person.getId().getId() == null) {
      throw new IllegalArgumentException("Must provide non null person id in person object");
    }
  }

  static void nonNullPersonId(UUID personId) {
    if (personId == null) {
      throw new IllegalArgumentException("Must provide a non-null personId");
    }
  }

  static void nonNullApplicationId(UUID applicationId) {
    if (applicationId == null) {
      throw new IllegalArgumentException("Must provide non-null applicationId");
    }
  }

  static void nonNullEnvironmentId(UUID eid) {
    if (eid == null) {
      throw new IllegalArgumentException("Must provide a non-null environment id");
    }
  }

  static <T> T readJsonValue(String content, Class<T> valueType) {
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

  static void nonNullServiceAccountId(UUID saId) {
    if (saId == null) {
      throw new IllegalArgumentException("You must provide a non null service account id");
    }
  }

  static void nonNullOrganisationId(UUID orgId) {
    if (orgId == null) {
      throw new IllegalArgumentException("Organisation ID is required");
    }
  }

  static void nonNullGroupId(UUID groupId) {
    if (groupId == null) {
      throw new IllegalArgumentException("Group ID is required");
    }
  }

  static void nonNullStrategyId(UUID strategyId) {
    if (strategyId == null) {
      throw new IllegalArgumentException("Strategy ID is required");
    }
  }

  // used so things that call toPerson multiple times can hold onto it
  UUID getOrganizationId();
  DbOrganization getDbOrganization();

  DbPerson byPerson(UUID id);

  DbPerson byPerson(UUID id, Opts opts);

  DbPerson byPerson(Person creator);

  DbRolloutStrategy byStrategy(UUID id);

  DbPortfolio byPortfolio(UUID portfolioId);

  DbEnvironment byEnvironment(UUID id);

  DbEnvironment byEnvironment(UUID id, Opts opts);

  DbApplication byApplication(UUID id);

  DbGroup byGroup(UUID gid, Opts opts);

  boolean personIsNotSuperAdmin(DbPerson person);

  boolean personIsSuperAdmin(DbPerson person);

  Group getSuperuserGroup(Opts opts);

  String limitLength(String s, int len);

  Environment toEnvironment(DbEnvironment env, Opts opts, Set<DbApplicationFeature> features);

  Environment toEnvironment(DbEnvironment env, Opts opts);

  String getCacheNameByEnvironment(DbEnvironment env);

  ServiceAccountPermission toServiceAccountPermission(
      DbServiceAccountEnvironment sae,
      Set<RoleType> rolePerms,
      boolean mustHaveRolePerms,
      Opts opt);

  ApplicationGroupRole applicationGroupRoleFromAcl(DbAcl acl);

  EnvironmentGroupRole environmentGroupRoleFromAcl(DbAcl acl);

  @NotNull List<RoleType> splitEnvironmentRoles(@Nullable String roles);

  List<ApplicationRoleType> splitApplicationRoles(String roles);

  EnvironmentGroupRole convertEnvironmentAcl(DbAcl dbAcl);

  OffsetDateTime toOff(LocalDateTime ldt);

  @NotNull String personName(@NotNull DbPerson person);
  @Nullable Person toPerson(@Nullable DbPerson person);

  @Nullable Person toPerson(@Nullable DbPerson dbp, @NotNull Opts opts);

  @Nullable Person toPerson(@Nullable DbPerson dbp, @Nullable DbOrganization org, @NotNull Opts opts);

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

  boolean isPersonApplicationAdmin(DbPerson dbPerson, DbApplication app);

  ServiceAccount toServiceAccount(DbServiceAccount sa, Opts opts);

  ServiceAccount toServiceAccount(
      DbServiceAccount sa, Opts opts, List<DbAcl> environmentsUserHasAccessTo);

  default List<RoleType> splitServiceAccountPermissions(String permissions) {
    // same now, were different historically
    return splitEnvironmentRoles(permissions);
  }

  FeatureEnvironment toFeatureEnvironment(
      @Nullable DbFeatureValue featureValue, @NotNull List<RoleType> roles, @NotNull DbEnvironment dbEnvironment,
      @NotNull Opts opts);

  FeatureValue toFeatureValue(DbApplicationFeature feature, DbFeatureValue value);

  FeatureValue toFeatureValue(DbApplicationFeature feature, DbFeatureValue value, Opts opts);

  RolloutStrategyInfo toRolloutStrategy(DbRolloutStrategy rs, Opts opts);
}
