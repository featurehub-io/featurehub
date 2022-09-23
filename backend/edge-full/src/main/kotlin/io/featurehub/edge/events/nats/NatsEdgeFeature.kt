package io.featurehub.edge.events.nats

import io.featurehub.edge.events.CloudEventsEdgePublisher
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class NatsEdgeFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (NATSFeature.isNatsConfigured()) {
      context.register(NATSFeature::class.java)

      context.register(object: AbstractBinder() {
        override fun configure() {
          bind(NatsFeatureUpdatePublisher::class.java).to(CloudEventsEdgePublisher::class.java).`in`(Singleton::class.java)
          bind(NatsFeatureListener::class.java).to(NatsFeatureListener::class.java).`in`(Immediate::class.java)
        }
      })

      return true
    }

    return false
  }
}
