package io.featurehub.db.publish.nats

import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds all possible caches and create filler listeners for them. We only do that for NATS for Dacha1 as
 * it is not required for Dacha2.
 */
@Singleton
@LifecyclePriority(priority = 5)
class NATSSetupCacheFillers @Inject constructor(
  cacheSource: CacheSource,
  natsServer: NATSSource,
  featureUpdateListener: FeatureUpdateListener
): LifecycleShutdown {
  private val namedCaches: MutableMap<String, NatsDachaCacheFiller> = ConcurrentHashMap()

  init {
    val id = UUID.randomUUID()

    // always listen to default
    namedCaches[ChannelConstants.DEFAULT_CACHE_NAME] = NatsDachaCacheFiller(
      ChannelConstants.DEFAULT_CACHE_NAME, natsServer, id, cacheSource,
      featureUpdateListener
    )
  }

  override fun shutdown() {
    namedCaches.values.parallelStream().forEach { obj: NatsDachaCacheFiller -> obj.close() }
  }
}
