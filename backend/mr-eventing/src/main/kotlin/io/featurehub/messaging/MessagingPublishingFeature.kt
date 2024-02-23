package io.featurehub.messaging

import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.messaging.service.FeatureMessagingPublisherConfiguration
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
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
        bind(FeatureMessagingCloudEventPublisherImpl::class.java)
          .to(FeatureMessagingPublisher::class.java)
          .to(FeatureMessagingPublisherConfiguration::class.java)
          .`in`(Singleton::class.java)
      }

    })


    return true
  }
}
