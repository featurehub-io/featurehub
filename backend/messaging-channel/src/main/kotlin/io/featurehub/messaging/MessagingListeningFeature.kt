package io.featurehub.messaging

import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.messaging.kinesis.KinesisFeatureMessagingListener
import io.featurehub.messaging.nats.NatsFeatureMessagingListener
import io.featurehub.messaging.pubsub.PubsubFeatureMessagingListener
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class MessagingListeningFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    ctx.register(object: AbstractBinder() {

      override fun configure() {
        bind(FeatureMessagingCloudEventPublisherImpl::class.java).to(FeatureMessagingCloudEventPublisher::class.java).`in`(
          Singleton::class.java)
        bind(MessagingConfigImpl::class.java).to(MessagingConfig::class.java).`in`(Singleton::class.java)

        if (NATSFeature.isNatsConfigured()) {
          bind(NatsFeatureMessagingListener::class.java).to(NatsFeatureMessagingListener::class.java).`in`(Immediate::class.java)
        }

        if (KinesisEventFeature.isEnabled()) {
          bind(KinesisFeatureMessagingListener::class.java).to(KinesisFeatureMessagingListener::class.java).`in`(
            Immediate::class.java)
        }

        if (GoogleEventFeature.isEnabled()) {
          bind(PubsubFeatureMessagingListener::class.java).to(PubsubFeatureMessagingListener::class.java).`in`(
            Immediate::class.java)
        }
      }

    })
    return true
  }
}
