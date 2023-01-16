package io.featurehub.enricher

import io.featurehub.enricher.kinesis.KinesisEnricherPublisher
import io.featurehub.enricher.nats.NatsEnricherPublisher
import io.featurehub.enricher.pubsub.PubsubEnricherPublisher
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class EnrichmentProcessingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    // we always bind this, clients should just not do anything if it is disabled (no binding of listeners, etc)
    ctx.register(object: AbstractBinder() {
      override fun configure() {
        bind(FeatureEnricherProcessor::class.java).to(FeatureEnricher::class.java).`in`(Singleton::class.java)

        // both dacha1 and dacha2 can publish from different mechanisms
        if (NATSFeature.isNatsConfigured()) {
          bind(NatsEnricherPublisher::class.java).to(NatsEnricherPublisher::class.java).`in`(Immediate::class.java)
        }

        if (KinesisEventFeature.isEnabled()) {
          bind(KinesisEnricherPublisher::class.java).to(KinesisEnricherPublisher::class.java).`in`(Immediate::class.java)
        }

        if (GoogleEventFeature.isEnabled()) {
          bind(PubsubEnricherPublisher::class.java).to(PubsubEnricherPublisher::class.java).`in`(Immediate::class.java)
        }
      }
    })

    return true
  }
}
