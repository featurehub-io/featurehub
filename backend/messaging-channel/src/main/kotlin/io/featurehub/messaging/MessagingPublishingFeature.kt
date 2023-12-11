package io.featurehub.messaging

import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.messaging.kinesis.KinesisFeatureMessagingPublisher
import io.featurehub.messaging.nats.NatsFeatureMessagingPublisher
import io.featurehub.messaging.pubsub.PubsubFeatureMessagingPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
import io.featurehub.publish.NATSFeature
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.apache.kafka.common.protocol.types.Field.Bool
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

data class MessagingDestination(val destination: String, val threadCount: Int)

class MessagingPublishingFeature : Feature {

  init {

  }

  override fun configure(ctx: FeatureContext): Boolean {
    ctx.register(object : AbstractBinder() {

      override fun configure() {
        bind(FeatureMessagingCloudEventPublisherImpl::class.java).to(FeatureMessagingCloudEventPublisher::class.java)
          .`in`(Singleton::class.java)
        bind(MessagingConfigImpl::class.java).to(MessagingConfig::class.java).`in`(Singleton::class.java)

        if (NATSFeature.isNatsConfigured()) {
          bind(NatsFeatureMessagingPublisher::class.java).to(NatsFeatureMessagingPublisher::class.java)
            .`in`(Immediate::class.java)
        }

        if (KinesisEventFeature.isEnabled()) {
          bind(KinesisFeatureMessagingPublisher::class.java).to(KinesisFeatureMessagingPublisher::class.java)
            .`in`(Immediate::class.java)
        }

        if (GoogleEventFeature.isEnabled()) {
          bind(PubsubFeatureMessagingPublisher::class.java).to(PubsubFeatureMessagingPublisher::class.java)
            .`in`(Immediate::class.java)
        }
      }

    })
    return true
  }

  companion object {
    val integrations: List<MessagingDestination>
    val publishing: Boolean

    init {
      integrations =
        listOf("integration.slack", "webhook.messaging", "integration.ms-teams").map { config(it) }.filterNotNull()
      publishing = integrations.isNotEmpty()
    }

    fun config(prefix: String): MessagingDestination? {
      val destination = FallbackPropertyConfig.getConfig("$prefix.destination")
      if (destination != null) {
        return MessagingDestination(
          destination,
          FallbackPropertyConfig.getConfig(
            "$prefix.threads",
            FallbackPropertyConfig.getConfig("messaging.default-threads", "5")
          ).toInt()
        )
      }
      return null
    }
  }
}
