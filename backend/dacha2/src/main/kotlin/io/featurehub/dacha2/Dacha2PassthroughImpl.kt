package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.enricher.EnrichmentEnvironment
import io.featurehub.utils.FallbackPropertyConfig
import java.util.UUID

class Dacha2PassthroughImpl(private val mrDacha2Api: Dacha2ServiceClient, private val featureValueFactory: FeatureValuesFactory) : Dacha2BaseCache() {
  private val apiKey = FallbackPropertyConfig.Companion.getConfig("dacha2.cache.api-key")

  override fun updateServiceAccount(serviceAccount: PublishServiceAccount) {
  }

  override fun updateEnvironment(env: PublishEnvironment) {
  }

  override fun updateFeature(feature: PublishFeatureValue) {
  }

  override fun getEnrichableEnvironment(eId: UUID): EnrichmentEnvironment {
    val envFeatures = findEnvironment(eId)
    return EnrichmentEnvironment(envFeatures.getFeatures(), envFeatures.environment)
  }

  private fun getEnvironment(eId: UUID): EnvironmentFeatures {
    val env = mrDacha2Api.getEnvironment(eId, apiKey).env
    return featureValueFactory.create(env)
  }

  private fun getServiceAccount(key: String): CacheServiceAccount {
    return mrDacha2Api.getServiceAccount(key, apiKey).serviceAccount
  }

  override fun getFeatureCollection(eId: UUID, apiKey: String): FeatureCollection? {
    val sa = getServiceAccount(apiKey)

    var collection: FeatureCollection? = null

    sa.permissions.find { it.environmentId == eId }?.let { perms ->
      if (perms.permissions.isNotEmpty()) {
        val features = getEnvironment(eId)
        collection = if (sa.filters != null && sa.filters!!.isNotEmpty()) {
          FeatureCollection(FilteredEnvironmentFeatures(features, sa.filters!!), perms, sa.id)
        } else {
          FeatureCollection(features, perms, sa.id)
        }
      }
    }

    return collection
  }

  override fun findEnvironment(eId: UUID): FeatureValues {
    return getEnvironment(eId)
  }

  override fun enableCache(cacheEnable: Boolean) {
  }

  override fun closeCache() {

  }
}
