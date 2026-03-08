package io.featurehub.messaging

import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.messaging.rest.SlackTestingHarnessResource
import io.featurehub.messaging.slack.SlackWebClient
import io.featurehub.messaging.utils.FeatureMessageFormatter
import io.featurehub.messaging.utils.FeatureMessageFormatterImpl
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InternalDeliveryFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(FeatureMessageFormatterImpl::class.java).to(FeatureMessageFormatter::class.java).`in`(Singleton::class.java)

      }
    })

    if (FallbackPropertyConfig.getConfig("testing-api-enabled", "false") == "true") {
      log.info("testing-api-enabled so enabling Slack Testing Harness")

      context.register(SlackTestingHarnessResource::class.java)
    }


    LifecycleListeners.starter(SlackWebClient::class.java, context)

    return true
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(InternalDeliveryFeature::class.java)
  }
}
