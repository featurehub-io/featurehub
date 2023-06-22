package io.featurehub.db.services

import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.db.api.Opts
import io.featurehub.db.model.*
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

interface Conversions {
  // used so things that call toPerson multiple times can hold onto it
  fun organizationId(): UUID
  fun dbOrganization(): DbOrganization
  fun hasOrganisation(): Boolean
  fun byPerson(id: UUID?): DbPerson?
  fun byPerson(id: UUID?, opts: Opts?): DbPerson?
  fun byPerson(creator: Person?): DbPerson?
  fun byStrategy(id: UUID?): DbRolloutStrategy?
  fun byPortfolio(portfolioId: UUID?): DbPortfolio?
  fun byEnvironment(id: UUID?): DbEnvironment?
  fun byEnvironment(id: UUID?, opts: Opts?): DbEnvironment?
  fun byApplication(id: UUID?): DbApplication?
  fun byGroup(gid: UUID?, opts: Opts?): DbGroup?
  fun personIsNotSuperAdmin(person: DbPerson?): Boolean

  fun safeConvert(bool: Boolean?): Boolean

  fun isPersonMemberOfPortfolioGroup(portfolioId: UUID, personId: UUID): Boolean
  fun isPersonMemberOfPortfolioAdminGroup(portfolioId: UUID, personId: UUID): Boolean
  fun personIsSuperAdmin(person: DbPerson?): Boolean
  fun personIsSuperAdmin(person: UUID): Boolean
  fun getSuperuserGroup(opts: Opts?): Group?
  fun limitLength(s: String?, len: Int): String?
  fun toEnvironment(env: DbEnvironment?, opts: Opts?, features: Set<DbApplicationFeature?>?): Environment?
  fun toEnvironment(env: DbEnvironment?, opts: Opts?): Environment?
  fun getCacheNameByEnvironment(env: DbEnvironment?): String?
  fun toServiceAccountPermission(
    sae: DbServiceAccountEnvironment?,
    rolePerms: Set<RoleType?>?,
    mustHaveRolePerms: Boolean,
    opt: Opts?
  ): ServiceAccountPermission?

  fun applicationGroupRoleFromAcl(acl: DbAcl?): ApplicationGroupRole
  fun environmentGroupRoleFromAcl(acl: DbAcl?): EnvironmentGroupRole
  fun splitEnvironmentRoles(roles: String?): MutableList<RoleType>
  fun splitApplicationRoles(roles: String?): MutableList<ApplicationRoleType>
  fun convertEnvironmentAcl(dbAcl: DbAcl?): EnvironmentGroupRole?
  fun toOff(ldt: LocalDateTime?): OffsetDateTime?
  fun personName(person: DbPerson): String
  fun toPerson(person: DbPerson?): Person?
  fun toPerson(dbp: DbPerson?, opts: Opts): Person?
  fun toPerson(dbp: DbPerson?, org: DbOrganization?, opts: Opts): Person?
  fun stripArchived(name: String, whenArchived: LocalDateTime?): String? {
    if (whenArchived == null) {
      return name
    }
    val prefix = name.indexOf(archivePrefix)
    return if (prefix == -1) {
      name
    } else name.substring(0, prefix)
  }

  fun toGroup(dbg: DbGroup?, opts: Opts?): Group?
  fun toApplication(app: DbApplication?, opts: Opts?): Application?
  fun toApplicationFeature(af: DbApplicationFeature?, opts: Opts?): Feature?
  fun toFeature(fs: DbFeatureValue?): Feature?
  fun toFeatureValue(fs: DbFeatureValue?): FeatureValue?
  fun toFeatureValue(fs: DbFeatureValue?, opts: Opts?): FeatureValue?
  fun toPortfolio(p: DbPortfolio?, opts: Opts?): Portfolio?
  fun toPortfolio(p: DbPortfolio?, opts: Opts?, person: Person?, personNotSuperAdmin: Boolean): Portfolio?
  fun toOrganization(org: DbOrganization?, opts: Opts?): Organization?
  fun isPersonApplicationAdmin(dbPerson: DbPerson?, app: DbApplication?): Boolean
  fun isPersonApplicationAdmin(personId: UUID, appId: UUID): Boolean
  fun toServiceAccount(sa: DbServiceAccount?, opts: Opts?): ServiceAccount?
  fun toServiceAccount(
    sa: DbServiceAccount?, opts: Opts?, environmentsUserHasAccessTo: List<DbAcl?>?
  ): ServiceAccount?

  fun splitServiceAccountPermissions(permissions: String?): List<RoleType>? {
    // same now, were different historically
    return splitEnvironmentRoles(permissions)
  }

  fun toFeatureEnvironment(
    featureValue: DbFeatureValue?, roles: List<RoleType?>, dbEnvironment: DbEnvironment,
    opts: Opts
  ): FeatureEnvironment

  fun toFeatureValue(feature: DbApplicationFeature?, value: DbFeatureValue?): FeatureValue?
  fun toFeatureValue(feature: DbApplicationFeature?, value: DbFeatureValue?, opts: Opts?): FeatureValue?
  fun toRolloutStrategy(rs: DbRolloutStrategy?, opts: Opts?): RolloutStrategyInfo?

  companion object {
    const val archivePrefix = ":\\:\\:"

    // in case a field can be a name or a uuid (i.e. feature key)
    @JvmStatic
    fun checkUuid(id: String?): UUID? {
      return if (id == null) {
        null
      } else try {
        UUID.fromString(id)
      } catch (e: Exception) {
        null
      }
    }

    @JvmStatic
    fun nonNullPortfolioId(portfolioId: UUID?) {
      requireNotNull(portfolioId) { "Must provide non null portfolioId" }
    }

    @JvmStatic
    fun nonNullPerson(person: Person?) {
      requireNotNull(person?.id?.id) { "Must provide non null person id in person object" }
    }

    @JvmStatic
    fun nonNullPersonId(personId: UUID?) {
      requireNotNull(personId) { "Must provide a non-null personId" }
    }

    @JvmStatic
    fun nonNullApplicationId(applicationId: UUID?) {
      requireNotNull(applicationId) { "Must provide non-null applicationId" }
    }

    fun nonNullEnvironmentId(eid: UUID?) {
      requireNotNull(eid) { "Must provide a non-null environment id" }
    }

    @JvmStatic
    fun <T> readJsonValue(content: String?, valueType: Class<T>?): T? {
      return try {
        CacheJsonMapper.mapper.readValue(content, valueType)
      } catch (e: JsonProcessingException) {
        null
      }
    }

    @JvmStatic
    fun valueToJsonString(environments: HiddenEnvironments?): String? {
      return try {
        CacheJsonMapper.mapper.writeValueAsString(environments)
      } catch (e: JsonProcessingException) {
        null
      }
    }

    fun nonNullServiceAccountId(saId: UUID?) {
      requireNotNull(saId) { "You must provide a non null service account id" }
    }

    @JvmStatic
    fun nonNullOrganisationId(orgId: UUID?) {
      requireNotNull(orgId) { "Organisation ID is required" }
    }

    @JvmStatic
    fun nonNullGroupId(groupId: UUID?) {
      requireNotNull(groupId) { "Group ID is required" }
    }

    fun nonNullStrategyId(strategyId: UUID?) {
      requireNotNull(strategyId) { "Strategy ID is required" }
    }
  }
}
