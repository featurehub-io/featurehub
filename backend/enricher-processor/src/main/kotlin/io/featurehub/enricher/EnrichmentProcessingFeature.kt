package io.featurehub.enricher

import io.featurehub.lifecycle.LifecycleListeners
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class EnrichmentProcessingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    // we always bind this, clients should just not do anything if it is disabled (no binding of listeners, etc)
    LifecycleListeners.starter(FeatureEnricherProcessor::class.java, ctx)

    // both dacha1 and dacha2 can publish from different mechanisms
//    if (NATSFeature.isNatsConfigured()) {
//      LifecycleListeners.starter(NatsEnricherPublisher::class.java, ctx)
//    }
//
//    if (KinesisEventFeature.isEnabled()) {
//      LifecycleListeners.starter(KinesisEnricherPublisher::class.java, ctx)
//    }
//
//    if (GoogleEventFeature.isEnabled()) {
//      LifecycleListeners.starter(PubsubEnricherPublisher::class.java, ctx)
//    }


    return true
  }
}
