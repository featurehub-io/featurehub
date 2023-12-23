package io.featurehub.events.pubsub

import io.featurehub.health.HealthSource
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.mr.events.listeners.PubsubChannelSubscribers
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class GoogleEventFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (isEnabled()) {
      context.register(object: AbstractBinder() {
        override fun configure() {
          bind(PubSubFactoryService::class.java).to(PubSubFactory::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
        }
      })

      // register it so others can register against it
      LifecycleListeners.starter(PubsubDynamicPublisher::class.java, context)
      // start listening to all the channels up front
      LifecycleListeners.starter(PubsubChannelSubscribers::class.java, context)

      return true
    }

    return false
  }

  companion object {
    fun isEnabled() = FallbackPropertyConfig.getConfig("cloudevents.pubsub.project") != null && FallbackPropertyConfig.getConfig("cloudevents.pubsub.enabled") == "true"
  }
}

