package io.featurehub.messaging

import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
import io.featurehub.messaging.service.FeatureMessagingPublisherConfiguration
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class MessagingPublishingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    ctx.register(object : AbstractBinder() {

      override fun configure() {
        // we have split this into two so db-sql doesn't have to depend on this
        // artifact to build, it separates cloud eventing from publishing
        val binding = bind(FeatureMessagingCloudEventPublisherImpl::class.java)
        boundInterfaces.forEach { binding.to(it) }
        binding.`in`(Singleton::class.java)
      }

    })


    return true
  }

  companion object {
    /**
     * Every interface this feature binds. Used by applications with more than one Jersey context to re-bind the
     * existing instances from the main context rather than creating a second set.
     */
    @JvmField
    val boundInterfaces: List<Class<*>> = listOf(
      FeatureMessagingPublisher::class.java,
      FeatureMessagingPublisherConfiguration::class.java,
    )
  }
}
