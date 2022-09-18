package io.featurehub.mr.events.common

import io.featurehub.mr.events.common.listeners.CloudEventListener
import io.featurehub.mr.events.common.listeners.CloudEventListenerImpl
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class CloudEventsCommonFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(CloudEventListenerImpl::class.java).to(CloudEventListener::class.java).`in`(Singleton::class.java)
      }

    })
    return true
  }
}
