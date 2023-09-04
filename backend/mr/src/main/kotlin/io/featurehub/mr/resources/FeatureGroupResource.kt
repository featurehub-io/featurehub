package io.featurehub.mr.resources

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.EnvironmentRoles
import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.mr.api.FeatureGroupServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.ApplicationUtils
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class FeatureGroupResource @Inject constructor(
  private val authManager: AuthManagerService,
  private val applicationUtils: ApplicationUtils,
  private val applicationApi: ApplicationApi,
  private val environmentApi: EnvironmentApi,
  private val featureGroupApi: FeatureGroupApi
) : FeatureGroupServiceDelegate {
  val defaultMax: Int
  private val log: Logger = LoggerFactory.getLogger(FeatureGroupResource::class.java)

  init {
    defaultMax = FallbackPropertyConfig.getConfig("feature-group.list.max", "20").toInt()
  }

  override fun createFeatureGroup(
    appId: UUID,
    featureGroup: FeatureGroupCreate,
    securityContext: SecurityContext
  ): FeatureGroupListGroup {
    val current = authManager.from(securityContext)
    allowedCreatePermissionsOnEnvironment(current, featureGroup.environmentId)

    try {
      val fg = featureGroupApi.createGroup(appId, current, featureGroup) ?: throw NotFoundException()

      return FeatureGroupListGroup()
        .name(fg.name)
        .environmentName(fg.environmentName)
        .environmentId(fg.environmentId)
        .order(fg.order)
        .hasStrategy(fg.strategies?.isNotEmpty() ?: false)
        .description(fg.description)
        .version(fg.version)
        .id(fg.id)
        .features(fg.features.map { FeatureGroupListFeature().key(it.key) })
    } catch (dne: FeatureGroupApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate Name", 409)
    }
  }

  private fun allowedCreatePermissionsOnEnvironment(current: Person, envId: UUID): EnvironmentRoles {
    val perms = environmentApi.personRoles(current, envId)

    log.debug("permissions are {}", perms)

    if (perms == null || !perms.environmentRoles.contains(RoleType.CHANGE_VALUE)) {
      throw ForbiddenException()
    }

    return perms
  }


  override fun deleteFeatureGroup(appId: UUID, fgId: UUID, securityContext: SecurityContext) {
    val current = authManager.from(securityContext)
    val group = featureGroupApi.getGroup(appId, fgId) ?: throw NotFoundException()

    allowedCreatePermissionsOnEnvironment(current, group.environmentId)

    if (!featureGroupApi.deleteGroup(appId, current, fgId)) {
      throw NotFoundException()
    }
  }

  override fun getFeatureGroup(appId: UUID, fgId: UUID, securityContext: SecurityContext): FeatureGroup {
    applicationUtils.featureReadCheck(securityContext, appId)

    return featureGroupApi.getGroup(appId, fgId) ?: throw NotFoundException()
  }

  override fun getFeatureGroupFeatures(
    appId: UUID,
    envId: UUID,
    securityContext: SecurityContext
  ): List<FeatureGroupFeature> {
    applicationUtils.featureReadCheck(securityContext, appId)

    return featureGroupApi.getFeaturesForEnvironment(appId, envId)
  }

  override fun listFeatureGroups(
    appId: UUID,
    holder: FeatureGroupServiceDelegate.ListFeatureGroupsHolder,
    securityContext: SecurityContext
  ): FeatureGroupList {
    val person = applicationUtils.featureReadCheck(securityContext, appId)

    val perms = applicationApi.findApplicationPermissions(appId, person.id!!.id)

    log.debug("permissions are {}", perms)

    if (perms.environments.isEmpty()) {
      return FeatureGroupList().count(0)
    }

    return featureGroupApi.listGroups(appId,
      holder.max ?: defaultMax,
        holder.filter,
      holder.page ?: 0, holder.sortOrder ?: SortOrder.ASC, holder.environmentId, perms)
  }

  override fun updateFeatureGroup(
    appId: UUID,
    fgId: UUID,
    featureGroupUpdate: FeatureGroupUpdate,
    securityContext: SecurityContext
  ): FeatureGroup {
    val current = authManager.from(securityContext)
    val group = featureGroupApi.getGroup(appId, fgId) ?: throw NotFoundException()

    allowedCreatePermissionsOnEnvironment(current, group.environmentId)

    try {
      return featureGroupApi.updateGroup(appId, current, fgId, featureGroupUpdate) ?: throw NotFoundException()
    } catch (oex: FeatureGroupApi.OptimisticLockingException) {
      throw WebApplicationException("Attemping to update old version", 412)
    }
  }
}
