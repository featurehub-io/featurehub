package io.featurehub.db.publish.nats

import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.common.CloudEventBroadcaster
import io.featurehub.mr.events.common.CloudEventBroadcasterWriter
import io.featurehub.mr.events.nats.NATSCloudEventsBroadcaster
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class NATSDachaEventingFeature : Feature {
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

    fun isDacha1Enabled(): Boolean =
      FallbackPropertyConfig.getConfig("dacha1.enabled") == "false"
  }

  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        if (isDacha2Enabled()) {
          // initial requests come in via REST, we only publish changes
          bind(NATSCloudEventsBroadcaster::class.java).to(CloudEventBroadcasterWriter::class.java).`in`(Singleton::class.java)
          bind(CloudEventBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
        }

        if (isDacha1Enabled()) {
          // we want this to fire up immediately on start and start listening
          bind(NATSSetupCacheFillers::class.java).to(NATSSetupCacheFillers::class.java).`in`(Immediate::class.java)
          bind(NATSDachaBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
        }
      }
    })

    return true
  }
}
