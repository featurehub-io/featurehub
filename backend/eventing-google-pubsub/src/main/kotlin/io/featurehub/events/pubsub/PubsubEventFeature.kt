package io.featurehub.events.pubsub

import io.featurehub.events.EventingFeatureSource
import io.featurehub.health.HealthSource
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class PubsubEventingFeatureSource : EventingFeatureSource {
  override val featureSource: Class<out Feature>?
    get() = if (PubsubEventFeature.isEnabled()) PubsubEventFeature::class.java else null
}

class PubsubEventFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (isEnabled()) {
      context.register(object: AbstractBinder() {
        override fun configure() {
          bind(PubSubFactoryService::class.java).to(PubSubFactory::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
        }
      })

      // register it so others can register against it
      LifecycleListeners.starter(PubsubDynamicPublisher::class.java, context)
      LifecycleListeners.starter(PubsubConfiguredSource::class.java, context)
      // start listening to all the channels up front
//      LifecycleListeners.starter(PubsubChannelSubscribers::class.java, context)

      return true
    }

    return false
  }

  companion object {
    fun isEnabled() = FallbackPropertyConfig.getConfig("cloudevents.pubsub.project") != null && FallbackPropertyConfig.getConfig("cloudevents.pubsub.enabled") == "true"
  }
}

