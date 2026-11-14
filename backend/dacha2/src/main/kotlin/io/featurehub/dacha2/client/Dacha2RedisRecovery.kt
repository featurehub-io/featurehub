package io.featurehub.dacha2.client

import io.featurehub.dacha2.Dacha2Cache
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * When MR comes back after an outage, drop the cache entries that were served from Redis so they are re-fetched
 * from MR (`dacha2.redis.evict-on-recovery`, default true).
 */
class Dacha2RedisRecovery @Inject constructor(
  private val client: Dacha2ServiceClient,
  private val cache: Dacha2Cache
) : LifecycleStarted {
  override fun started() {
    if (client !is Dacha2FallbackServiceClient) return

    if (FallbackPropertyConfig.getConfig("dacha2.redis.evict-on-recovery", "true") != "true") {
      log.info("dacha2: entries served from redis will be kept when the management repository recovers")
      return
    }

    client.addRecoveryListener { envIds, apiKeys ->
      log.info("dacha2: evicting {} environments and {} api keys that were served from redis", envIds.size, apiKeys.size)
      cache.invalidateEntries(envIds, apiKeys)
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Dacha2RedisRecovery::class.java)
  }
}
