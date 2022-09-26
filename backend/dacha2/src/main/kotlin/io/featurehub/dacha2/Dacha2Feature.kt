package io.featurehub.dacha2

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha2.kinesis.KinesisDachaEventsListener
import io.featurehub.dacha2.nats.NatsDachaEventsListener
import io.featurehub.dacha2.pubsub.PubsubDachaEventsListener
import io.featurehub.dacha2.resource.DachaApiKeyResource
import io.featurehub.dacha2.resource.DachaEnvironmentResource
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
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
        bind(Dacha2CloudEventListenerImpl::class.java).to(Dacha2CloudEventListener::class.java).`in`(Immediate::class.java)
        bind(FeatureValuesFactoryImpl::class.java).to(FeatureValuesFactory::class.java).`in`(Singleton::class.java)
        bind(DachaApiKeyResource::class.java).to(DachaApiKeyService::class.java).`in`(Singleton::class.java)

        if (GoogleEventFeature.isEnabled()) {
          // bind and start listening immediately
          bind(PubsubDachaEventsListener::class.java).to(PubsubDachaEventsListener::class.java).`in`(Immediate::class.java)
        }

        if (KinesisEventFeature.isEnabled()) {
          bind(KinesisDachaEventsListener::class.java).to(KinesisDachaEventsListener::class.java).`in`(Immediate::class.java)
        }
      }
    })

    return true
  }
}
