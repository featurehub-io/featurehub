package io.featurehub.enricher

import io.featurehub.enricher.kinesis.KinesisEnricherListener
import io.featurehub.enricher.nats.NatsEnricherListener
import io.featurehub.enricher.pubsub.PubsubEnricherListener
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.publish.NATSFeature
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class EnricherListenerFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    val wireRequired = EnricherConfig.enabled() && (NATSFeature.isNatsConfigured() || KinesisEventFeature.isEnabled() || GoogleEventFeature.isEnabled())

    if (wireRequired) {
      context.register(object: AbstractBinder() {
        override fun configure() {
          if (NATSFeature.isNatsConfigured()) {
            bind(NatsEnricherListener::class.java).to(NatsEnricherListener::class.java).`in`(Immediate::class.java)
          }
          if (KinesisEventFeature.isEnabled()) {
            bind(KinesisEnricherListener::class.java).to(KinesisEnricherListener::class.java).`in`(Immediate::class.java)
          }
          if (GoogleEventFeature.isEnabled()) {
            bind(PubsubEnricherListener::class.java).to(PubsubEnricherListener::class.java).`in`(Immediate::class.java)
          }
        }
      })
    }

    return wireRequired
  }
}
