package io.featurehub.enricher

import io.featurehub.enricher.kinesis.KinesisEnricherListener
import io.featurehub.enricher.nats.NatsEnricherListener
import io.featurehub.enricher.pubsub.PubsubEnricherListener
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.publish.NATSFeature
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class EnricherListenerFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    val wireRequired = EnricherConfig.enabled() && (NATSFeature.isNatsConfigured() || KinesisEventFeature.isEnabled() || GoogleEventFeature.isEnabled())

    if (wireRequired) {
      if (NATSFeature.isNatsConfigured()) {
        LifecycleListeners.wrap(NatsEnricherListener::class.java, context)
      }
      if (KinesisEventFeature.isEnabled()) {
        LifecycleListeners.starter(KinesisEnricherListener::class.java, context)
      }
      if (GoogleEventFeature.isEnabled()) {
        LifecycleListeners.starter(PubsubEnricherListener::class.java, context)
      }

    }

    return wireRequired
  }
}
