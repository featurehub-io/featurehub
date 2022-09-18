package io.featurehub.edge.events

import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class NatsFeature : Feature {
  companion object {
    fun isNatsEnabled() = FallbackPropertyConfig.getConfig("nats.urls") != null
  }

  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(NatsFeatureUpdatePublisher::class.java).to(CloudEventsEdgePublisher::class.java).`in`(Singleton::class.java)
        bind(NatsFeatureListener::class.java).to(NatsFeatureListener::class.java).`in`(Immediate::class.java)
      }
    })

    return true
  }
}
