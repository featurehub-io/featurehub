package io.featurehub.edge

import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.services.Conversions
import io.featurehub.db.services.ConvertUtils
import io.featurehub.db.services.TestSDKFeatureUpdateServiceImpl
import io.featurehub.db.services.UpdateFeatureApi
import io.featurehub.db.services.UpdateFeatureApiImpl
import io.featurehub.edge.db.sql.EdgeRestWebhookEncryptionService
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.mr.events.common.listeners.FoundationFeatureUpdateListenerImpl
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

/**
 * This is used when EdgeRest is used on its own, otherwise Party-Server-Rest wires up certain things provided here.
 */
class EdgeRestFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(ConvertUtils::class.java).to(Conversions::class.java).`in`(
          Singleton::class.java
        )
        bind(UpdateFeatureApiImpl::class.java).to(UpdateFeatureApi::class.java).`in`(Singleton::class.java)
        bind(TestSDKFeatureUpdateServiceImpl::class.java).to(FeatureUpdateBySDKApi::class.java).`in`(Singleton::class.java)

        bind(EdgeRestWebhookEncryptionService::class.java)
          .to(WebhookEncryptionService::class.java).`in`(Singleton::class.java)
      }
    })

    return true

  }
}
