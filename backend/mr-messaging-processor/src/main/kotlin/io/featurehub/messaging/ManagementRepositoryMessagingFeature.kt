package io.featurehub.messaging

import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.messaging.converter.FeatureMessagingConverter
import io.featurehub.messaging.converter.FeatureMessagingConverterImpl
import io.featurehub.messaging.kinesis.KinesisFeatureMessagingPublisher
import io.featurehub.messaging.nats.NatsFeatureMessagingPublisher
import io.featurehub.messaging.pubsub.PubsubFeatureMessagingPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
import io.featurehub.publish.NATSFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class ManagementRepositoryMessagingFeature: Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    // MR only publishes
    ctx.register(MessagingPublishingFeature::class.java)

    ctx.register(object: AbstractBinder() {
      override fun configure() {
        bind(FeatureMessagingConverterImpl::class.java).to(FeatureMessagingConverter::class.java).`in`(Singleton::class.java)
      }
    })
    return true
  }
}
