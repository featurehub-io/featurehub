package io.featurehub.messaging

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.messaging.slack.SlackWebClient
import io.featurehub.messaging.utils.FeatureMessageFormatter
import io.featurehub.messaging.utils.FeatureMessageFormatterImpl
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class InternalDeliveryFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(FeatureMessageFormatterImpl::class.java).to(FeatureMessageFormatter::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(SlackWebClient::class.java, context)

    return true
  }
}
