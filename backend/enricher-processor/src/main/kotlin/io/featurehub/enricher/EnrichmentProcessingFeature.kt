package io.featurehub.enricher

import io.featurehub.enricher.kinesis.KinesisEnricherPublisher
import io.featurehub.enricher.nats.NatsEnricherPublisher
import io.featurehub.enricher.pubsub.PubsubEnricherPublisher
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class EnrichmentProcessingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    // we always bind this, clients should just not do anything if it is disabled (no binding of listeners, etc)
    ctx.register(object: AbstractBinder() {
      override fun configure() {
        bind(FeatureEnricherProcessor::class.java).to(FeatureEnricher::class.java).`in`(Singleton::class.java)
      }
    })

    // both dacha1 and dacha2 can publish from different mechanisms
    if (NATSFeature.isNatsConfigured()) {
      LifecycleListeners.starter(NatsEnricherPublisher::class.java, ctx)
    }

    if (KinesisEventFeature.isEnabled()) {
      LifecycleListeners.starter(KinesisEnricherPublisher::class.java, ctx)
    }

    if (GoogleEventFeature.isEnabled()) {
      LifecycleListeners.starter(PubsubEnricherPublisher::class.java, ctx)
    }


    return true
  }
}
