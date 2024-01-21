package io.featurehub.db.publish.nats

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.nats.NatsCloudEventsPublishers
import io.featurehub.mr.events.nats.NatsMRCloudEventsQueueUpdateListener
import io.featurehub.publish.NATSFeature
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class NatsDachaEventingFeature : Feature {
  companion object {
    // we use this only if nats is configured
    fun isEnabled() : Boolean {
      return FallbackPropertyConfig.getConfig("nats.urls") != null
    }

    // enabled by default
    fun isDacha1Enabled(): Boolean =
      FallbackPropertyConfig.getConfig("dacha1.enabled") != "false"
  }

  override fun configure(context: FeatureContext): Boolean {
    if (NATSFeature.isNatsConfigured()) {
      context.register(object : AbstractBinder() {
        override fun configure() {
          if (isDacha1Enabled()) {
            bind(NatsDachaBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
          }
        }
      })

      // this binds all of the respective cloud events to appropriate outbound channels
//      LifecycleListeners.starter(NatsCloudEventsPublishers::class.java, context)
      // always listen for Edge updates in Cloud Events format, use wrap even tho it doesn't have a startup method
//      LifecycleListeners.wrap(NatsMRCloudEventsQueueUpdateListener::class.java, context)

      if (isDacha1Enabled()) {
        // we want this to fire up immediately on start and start listening. It listens from edge for updates
        // about feature updates and webhook success/failure
        LifecycleListeners.wrap(NATSSetupCacheFillers::class.java, context)
      }

      return true
    }

    return false
  }
}
