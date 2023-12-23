package io.featurehub.messaging

import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

data class MessagingDestination(val destination: String, val threadCount: Int)

class MessagingPublishingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    ctx.register(object : AbstractBinder() {

      override fun configure() {
        bind(FeatureMessagingCloudEventPublisherImpl::class.java).to(FeatureMessagingCloudEventPublisher::class.java)
          .`in`(Singleton::class.java)
        bind(MessagingConfigImpl::class.java).to(MessagingConfig::class.java).`in`(Singleton::class.java)
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
