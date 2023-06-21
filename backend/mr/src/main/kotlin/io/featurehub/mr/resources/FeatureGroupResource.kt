package io.featurehub.mr.resources

import io.featurehub.db.api.FeatureGroupApi
import io.featurehub.mr.api.FeatureGroupServiceDelegate
import io.featurehub.mr.model.*
import io.featurehub.mr.utils.ApplicationUtils
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.SecurityContext
import java.util.*


class FeatureGroupResource @Inject constructor(
  private val applicationUtils: ApplicationUtils,
  private val featureGroupApi: FeatureGroupApi
) : FeatureGroupServiceDelegate {
  val defaultMax: Int

  init {
    defaultMax = FallbackPropertyConfig.getConfig("feature-group.list.max", "20").toInt()
  }

  override fun createFeatureGroup(
    appId: UUID,
    featureGroup: FeatureGroupCreate,
    securityContext: SecurityContext
  ): FeatureGroupListGroup {

    val check = applicationUtils.featureCreatorCheck(securityContext, appId)

    try {
      val fg = featureGroupApi.createGroup(appId, check.current, featureGroup) ?: throw NotFoundException()

      return FeatureGroupListGroup()
        .name(fg.name)
        .environmentName(fg.environmentName)
        .environmentId(fg.environmentId)
        .order(fg.order)
        .hasStrategy(fg.hasStrategy)
        .description(fg.description)
        .version(fg.version)
        .id(fg.id)
        .features(fg.features.map { FeatureGroupListFeature().key(it.key) })
    } catch (dne: FeatureGroupApi.DuplicateNameException) {
      throw WebApplicationException("Duplicate Name", 409)
    }
  }

  override fun deleteFeatureGroup(appId: UUID, fgId: UUID, securityContext: SecurityContext) {
    val check = applicationUtils.featureCreatorCheck(securityContext, appId)

    if (!featureGroupApi.deleteGroup(appId, check.current, fgId)) {
      throw NotFoundException()
    }
  }

  override fun getFeatureGroup(appId: UUID, fgId: UUID, securityContext: SecurityContext): FeatureGroup {
    val check = applicationUtils.featureCreatorCheck(securityContext, appId)

    return featureGroupApi.getGroup(appId, check.current, fgId) ?: throw NotFoundException()
  }

  override fun listFeatureGroups(
    appId: UUID,
    holder: FeatureGroupServiceDelegate.ListFeatureGroupsHolder,
    securityContext: SecurityContext
  ): FeatureGroupList {
    val check = applicationUtils.featureCreatorCheck(securityContext, appId)

    return featureGroupApi.listGroups(appId,
      holder.max ?: defaultMax,
        holder.filter,
      holder.page ?: 0, holder.sortOrder ?: SortOrder.ASC, holder.environmentId)
  }

  override fun updateFeatureGroup(
    appId: UUID,
    fgId: UUID,
    featureGroupUpdate: FeatureGroupUpdate,
    securityContext: SecurityContext
  ): FeatureGroup {
    val check = applicationUtils.featureCreatorCheck(securityContext, appId)
    try {
      return featureGroupApi.updateGroup(appId, check.current, fgId, featureGroupUpdate) ?: throw NotFoundException()
    } catch (oex: FeatureGroupApi.OptimisticLockingException) {
      throw WebApplicationException("Attemping to update old version", 412)
    }
  }
}
