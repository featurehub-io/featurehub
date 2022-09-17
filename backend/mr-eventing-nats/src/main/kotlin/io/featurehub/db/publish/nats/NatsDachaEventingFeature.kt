package io.featurehub.db.publish.nats

import io.featurehub.mr.events.common.*
import io.featurehub.mr.events.common.listeners.CloudEventListener
import io.featurehub.mr.events.common.listeners.CloudEventListenerImpl
import io.featurehub.mr.events.nats.NatsCloudEventsDachaChannel
import io.featurehub.mr.events.nats.NatsCloudEventsEdgeChannel
import io.featurehub.mr.events.nats.NatsMRCloudEventsQueueUpdateListener
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

    /*
    We need to be able to support both Dacha 1 and Dacha 2 at the same time for rollover.
     */
    fun isDacha2Enabled(): Boolean =
      FallbackPropertyConfig.getConfig("dacha2.enabled") == "true"

    // enabled by default
    fun isDacha1Enabled(): Boolean =
      FallbackPropertyConfig.getConfig("dacha1.enabled") != "false"
  }

  override fun configure(context: FeatureContext): Boolean {
    // this excludes the clloudevent cache broadcaster which is only wired on dacha2
    context.register(CloudEventsCommonFeature::class.java)

    context.register(object: AbstractBinder() {
      override fun configure() {
        if (isDacha2Enabled()) {
          // initial requests come in via REST, we only publish changes
          bind(NatsCloudEventsEdgeChannel::class.java).to(CloudEventsEdgeChannel::class.java).`in`(Singleton::class.java)
          bind(NatsCloudEventsDachaChannel::class.java).to(CloudEventsDachaChannel::class.java).`in`(Singleton::class.java)
          bind(CloudEventCacheBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
        }

        // always listen for Edge updates in Cloud Events format
        bind(NatsMRCloudEventsQueueUpdateListener::class.java).to(NatsMRCloudEventsQueueUpdateListener::class.java).`in`(Immediate::class.java)

        if (isDacha1Enabled()) {
          // we want this to fire up immediately on start and start listening. It listens from edge for updates
          // about feature updates and webhook success/failure
          bind(NATSSetupCacheFillers::class.java).to(NATSSetupCacheFillers::class.java).`in`(Immediate::class.java)
          bind(NatsDachaBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
        }
      }
    })

    return true
  }
}
