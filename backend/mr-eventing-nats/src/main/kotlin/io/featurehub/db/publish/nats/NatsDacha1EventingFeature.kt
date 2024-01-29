package io.featurehub.db.publish.nats

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.publish.NATSFeature
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class NatsDacha1EventingFeature : Feature {
  companion object {
    // enabled by default
    fun isDacha1Enabled(): Boolean =
      FallbackPropertyConfig.getConfig("dacha1.enabled") != "false"
  }

  override fun configure(context: FeatureContext): Boolean {
    if (!NATSFeature.isNatsConfigured() || !isDacha1Enabled()) return false

    context.register(object : AbstractBinder() {
      override fun configure() {
        if (isDacha1Enabled()) {
          bind(NatsDachaBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
        }
      }
    })

    // we want this to fire up immediately on start and start listening. It listens from edge for updates
    // about feature updates and webhook success/failure
    LifecycleListeners.wrap(NATSSetupCacheFillers::class.java, context)

    return true
  }
}
