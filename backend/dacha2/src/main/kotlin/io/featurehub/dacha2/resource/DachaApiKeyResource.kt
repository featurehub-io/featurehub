package io.featurehub.dacha2.resource

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.dacha.model.DachaPermissionResponse
import io.featurehub.dacha2.Dacha2Cache
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import java.util.*

class DachaApiKeyResource @Inject constructor(private val cache: Dacha2Cache) : DachaApiKeyService {

  override fun getApiKeyDetails(eId: UUID, serviceAccountKey: String, excludeRetired: Boolean?): DachaKeyDetailsResponse {
    val collection = cache.getFeatureCollection(eId, serviceAccountKey) ?: throw NotFoundException()

    val environment = collection.features.environment
    val pureFeatureList = collection.features.getFeatures()
    val filteredList = if (excludeRetired == true) pureFeatureList.filter { it.value?.retired != true } else pureFeatureList
    return DachaKeyDetailsResponse()
      .organizationId(environment.organizationId)
      .portfolioId(environment.portfolioId)
      .applicationId(environment.applicationId)
      .serviceKeyId(collection.serviceAccountId)
      .etag(collection.features.getEtag())
      .environmentInfo(environment.environment.environmentInfo)
      .features(if (collection.perms.permissions.contains(RoleType.EXTENDED_DATA)) filteredList.toList() else stripExtendedData(filteredList))
  }

  private fun stripExtendedData(filteredList: Collection<CacheEnvironmentFeature>): List<CacheEnvironmentFeature> {
    return filteredList.map { ef -> if (ef.featureProperties?.isNotEmpty() == true) stripFeatureProperties(ef) else ef }.toList()
  }

  private fun stripFeatureProperties(ef: CacheEnvironmentFeature): CacheEnvironmentFeature {
    return CacheEnvironmentFeature().feature(ef.feature).value(ef.value)
  }


  // this is used by the PUT api
  override fun getApiKeyPermissions(eId: UUID, serviceAccountKey: String, featureKey: String): DachaPermissionResponse {
    val collection = cache.getFeatureCollection(eId, serviceAccountKey) ?: throw NotFoundException()

    val feature = collection.features.getFeatures().find { fv -> fv.feature.key == featureKey } ?: throw NotFoundException()

    val environment = collection.features.environment
    return DachaPermissionResponse()
      .organizationId(environment.organizationId)
      .portfolioId(environment.portfolioId)
      .applicationId(environment.applicationId)
      .serviceKeyId(collection.serviceAccountId)
      .roles(collection.perms.permissions)
      .environmentInfo(environment.environment.environmentInfo)
      .feature(if (collection.perms.permissions.contains(RoleType.EXTENDED_DATA)) feature else stripFeatureProperties(feature))
  }
}
