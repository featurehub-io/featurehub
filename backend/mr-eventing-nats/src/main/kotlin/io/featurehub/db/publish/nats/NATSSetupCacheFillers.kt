package io.featurehub.db.publish.nats

import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import cd.connect.lifecycle.LifecycleTransition
import io.featurehub.db.model.DbNamedCache
import io.featurehub.db.model.query.QDbNamedCache
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.listeners.FeatureUpdateFactory
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
class NATSSetupCacheFillers @Inject constructor(
  cacheSource: CacheSource,
  natsServer: NATSSource, featureUpdateFactory: FeatureUpdateFactory
) {
  private val namedCaches: MutableMap<String, NatsDachaCacheFiller> = ConcurrentHashMap()

  init {
    val id = UUID.randomUUID()

    val featureUpdateListener = featureUpdateFactory.createListener()

    // always listen to default
    if (QDbNamedCache().findCount() == 0) {
      namedCaches[ChannelConstants.DEFAULT_CACHE_NAME] = NatsDachaCacheFiller(
        ChannelConstants.DEFAULT_CACHE_NAME, natsServer, id, cacheSource,
        featureUpdateListener
      )
    }

    QDbNamedCache().findList().forEach { nc: DbNamedCache ->
      namedCaches[nc.cacheName] = NatsDachaCacheFiller(nc.cacheName, natsServer, id, cacheSource, featureUpdateListener)
    }

    ApplicationLifecycleManager.registerListener { trans: LifecycleTransition ->
      if (trans.next == LifecycleStatus.TERMINATING) {
        shutdown()
      }
    }
  }

  private fun shutdown() {
    namedCaches.values.parallelStream().forEach { obj: NatsDachaCacheFiller -> obj.close() }
  }
}
