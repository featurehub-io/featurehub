package io.featurehub.dacha2

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.caching.FastlyPublisher
import io.featurehub.dacha2.resource.DachaApiKeyResource
import io.featurehub.dacha2.resource.DachaEnvironmentResource
import io.featurehub.enricher.FeatureEnrichmentCache
import io.featurehub.enricher.EnrichmentProcessingFeature
import io.featurehub.lifecycle.LifecycleListeners
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class Dacha2Feature : Feature {
  // depends on OpenTelemetry,
  override fun configure(context: FeatureContext): Boolean {
    context.register(DachaApiKeyResource::class.java)
    context.register(DachaEnvironmentResource::class.java)
    context.register(EnrichmentProcessingFeature::class.java)


    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(Dacha2CacheImpl::class.java).to(Dacha2Cache::class.java).to(
          FeatureEnrichmentCache::class.java).`in`(Singleton::class.java)
        if (FastlyPublisher.fastlyEnabled()) {
          bind(FastlyPublisher::class.java).to(Dacha2CacheListener::class.java).`in`(Singleton::class.java)
        }
        bind(FeatureValuesFactoryImpl::class.java).to(FeatureValuesFactory::class.java).`in`(Singleton::class.java)
        bind(DachaApiKeyResource::class.java).to(DachaApiKeyService::class.java).`in`(Singleton::class.java)
      }
    })

//    if (NatsDachaEventsListener.isEnabled()) {
//      context.register(NatsDachaEventsListener::class.java)
//    }

    LifecycleListeners.starter(Dacha2CloudEventListenerImpl::class.java, context)

//    if (GoogleEventFeature.isEnabled()) {
//      LifecycleListeners.starter(PubsubDachaCloudEvents::class.java, context)
//    }
//
//    if (KinesisEventFeature.isEnabled()) {
//      LifecycleListeners.starter(KinesisDachaCloudEvents::class.java, context)
//    }

    return true
  }
}
