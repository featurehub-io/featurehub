package io.featurehub.dacha2

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.enricher.EnrichmentEnvironment
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class Dacha2DelegatingCache @Inject constructor(private val mrDacha2Api: Dacha2ServiceClient,
                                                private val featureValueFactory: FeatureValuesFactory) : Dacha2BaseCache() {
  private var cache: Dacha2BaseCache
  private var cacheEnabled: Boolean = false
  private val log: Logger = LoggerFactory.getLogger(Dacha2DelegatingCache::class.java)

  init {
    cache = Dacha2PassthroughImpl(mrDacha2Api, featureValueFactory)
    log.info("started dacha2 in uncached passthrough mode (waiting on confirmed connection to streaming layer)")
  }

  override fun updateServiceAccount(serviceAccount: PublishServiceAccount) {
    cache.updateServiceAccount(serviceAccount)
  }

  override fun updateEnvironment(env: PublishEnvironment) {
    cache.updateEnvironment(env)
  }

  override fun updateFeature(feature: PublishFeatureValue) {
    cache.updateFeature(feature)
  }

  override fun getFeatureCollection(eId: UUID, apiKey: String): FeatureCollection? {
    return cache.getFeatureCollection(eId, apiKey)
  }

  override fun findEnvironment(eId: UUID): FeatureValues {
    return cache.findEnvironment(eId)
  }

  override fun enableCache(cacheEnable: Boolean) {
    if (cacheEnabled && !cacheEnable) {
      log.info("lost connectivity, swapping dacha2 in uncached passthrough mode")
      cache.closeCache()
      cache = Dacha2PassthroughImpl(mrDacha2Api, featureValueFactory)
    } else if (!cacheEnabled && cacheEnable) {
      log.info("connectivity to streaming established, swapping dacha2 to cached mode (empty cache)")
      if (Dacha2Utility.usingGuavaCache) {
        cache = Dacha2CacheImpl(mrDacha2Api, featureValueFactory)
      } else {
        cache = Dacha2NewCacheImpl(mrDacha2Api, featureValueFactory)
      }
    }

    cacheEnabled = cacheEnable
  }

  override fun closeCache() {
    cache.closeCache()
  }

  override fun getEnrichableEnvironment(eId: UUID): EnrichmentEnvironment {
    return cache.getEnrichableEnvironment(eId)
  }
}
