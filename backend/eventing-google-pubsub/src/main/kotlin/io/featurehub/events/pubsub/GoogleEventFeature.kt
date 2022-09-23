package io.featurehub.events.pubsub

import io.featurehub.health.HealthSource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class GoogleEventFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (isEnabled()) {

      context.register(object: AbstractBinder() {
        override fun configure() {
          bind(PubSubFactoryService::class.java).to(PubSubFactory::class.java).to(HealthSource::class.java)
        }
      })

      return true
    }

    return false
  }

  companion object {
    fun isEnabled() = FallbackPropertyConfig.getConfig("cloudevents.pubsub.project") != null && FallbackPropertyConfig.getConfig("cloudevents.pubsub.enabled") == "true"
  }
}
