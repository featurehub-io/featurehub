package io.featurehub.dacha2.resource

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.dacha.model.DachaPermissionResponse
import io.featurehub.dacha2.Dacha2Cache
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
      .features(filteredList.toList())
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
      .feature(feature)
  }
}
