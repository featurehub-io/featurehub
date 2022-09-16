package io.featurehub.events

import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class CloudEventsFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(CloudEventTelemetryWrapperImpl::class.java).to(CloudEventTelemetryWrapper::class.java).`in`(Singleton::class.java)
        bind(CloudEventsTelemetryWrapperImpl::class.java).to(CloudEventsTelemetryWrapper::class.java).`in`(Singleton::class.java)
      }
    })
    return true
  }
}
