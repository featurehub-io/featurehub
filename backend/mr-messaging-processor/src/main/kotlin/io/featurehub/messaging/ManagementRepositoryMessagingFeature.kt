package io.featurehub.messaging

import io.featurehub.messaging.converter.FeatureMessagingConverter
import io.featurehub.messaging.converter.FeatureMessagingConverterImpl
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
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
