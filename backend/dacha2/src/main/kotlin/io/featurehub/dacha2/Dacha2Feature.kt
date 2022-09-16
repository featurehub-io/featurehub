package io.featurehub.dacha2

import io.featurehub.dacha2.nats.NatsDachaEventsListener
import io.featurehub.dacha2.resource.DachaApiKeyResource
import io.featurehub.dacha2.resource.DachaEnvironmentResource
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class Dacha2Feature : Feature {
  // depends on OpenTelemetry,
  override fun configure(context: FeatureContext): Boolean {
    context.register(DachaApiKeyResource::class.java)
    context.register(DachaEnvironmentResource::class.java)

    if (NatsDachaEventsListener.isEnabled()) {
      context.register(NatsDachaEventsListener::class.java)
    }

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(Dacha2CacheImpl::class.java).to(Dacha2Cache::class.java).`in`(Singleton::class.java)
        bind(Dacha2CloudEventListenerImpl::class.java).to(Dacha2CloudEventListener::class.java).`in`(Singleton::class.java)
        bind(FeatureValuesFactoryImpl::class.java).to(FeatureValuesFactory::class.java).`in`(Singleton::class.java)
      }

    })

    return true
  }
}
