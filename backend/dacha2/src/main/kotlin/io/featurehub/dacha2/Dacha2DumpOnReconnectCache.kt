package io.featurehub.dacha2

import io.featurehub.dacha2.api.Dacha2ServiceClient
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class Dacha2DumpOnReconnectCache @Inject constructor(mrDacha2Api: Dacha2ServiceClient,
                                                     featureValueFactory: FeatureValuesFactory) : Dacha2CacheImpl(mrDacha2Api, featureValueFactory) {
  private var cacheEnabled = true
  private val log: Logger = LoggerFactory.getLogger(Dacha2DumpOnReconnectCache::class.java)

  init {
    log.info("Dacha2 - using dump on streaming layer reconnect")
  }

  override fun enableCache(cacheEnable: Boolean) {
    // if we are being enabled and we were "disabled", we dump the cache on this situation
    if (cacheEnable && !cacheEnabled) {
      log.info("streaming has reconnected, dumping cache as assuming potential poisoning")

      resetCache()
    }

    if (!cacheEnable) {
      log.info("streaming layer has gone away, continuning to serve from cache and will drop cache when reconnected")
    }

    cacheEnabled = cacheEnable
  }
}


class Dacha2NewDumpOnReconnectCache @Inject constructor(mrDacha2Api: Dacha2ServiceClient,
                                                     featureValueFactory: FeatureValuesFactory) : Dacha2NewCacheImpl(mrDacha2Api, featureValueFactory) {
  private var cacheEnabled = true
  private val log: Logger = LoggerFactory.getLogger(Dacha2DumpOnReconnectCache::class.java)

  init {
    log.info("Dacha2 - using dump on streaming layer reconnect")
  }

  override fun enableCache(cacheEnable: Boolean) {
    // if we are being enabled and we were "disabled", we dump the cache on this situation
    if (cacheEnable && !cacheEnabled) {
      log.info("streaming has reconnected, dumping cache as assuming potential poisoning")

      resetCache()
    }

    if (!cacheEnable) {
      log.info("streaming layer has gone away, continuning to serve from cache and will drop cache when reconnected")
    }

    cacheEnabled = cacheEnable
  }
}
