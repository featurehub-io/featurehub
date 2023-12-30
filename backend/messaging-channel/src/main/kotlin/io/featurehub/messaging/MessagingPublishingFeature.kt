package io.featurehub.messaging

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.messaging.service.FeatureMessagingCloudEventInitializer
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisherImpl
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class MessagingPublishingFeature : Feature {
  override fun configure(ctx: FeatureContext): Boolean {
    ctx.register(object : AbstractBinder() {

      override fun configure() {
        bind(FeatureMessagingCloudEventPublisherImpl::class.java).to(FeatureMessagingCloudEventPublisher::class.java)
          .`in`(Singleton::class.java)
      }

    })

    LifecycleListeners.starter(FeatureMessagingCloudEventInitializer::class.java, ctx)

    return true
  }
}
