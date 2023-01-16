package io.featurehub.mr.webhook

import io.featurehub.db.api.WebhookApi
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.webhook.events.WebhookEnvironmentResult
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManagementRepositoryWebhookFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(WebhookListeners::class.java).to(WebhookListeners::class.java).`in`(Immediate::class.java)
      }
    })

    return true
  }
}

class WebhookListeners @Inject constructor(cloudEventsRegistry: CloudEventReceiverRegistry, webhookApi: WebhookApi) {
  private val log: Logger = LoggerFactory.getLogger(WebhookListeners::class.java)
  init {
    cloudEventsRegistry.listen(WebhookEnvironmentResult::class.java) { hook, _ ->
      log.debug("webhook: {} >> {}", hook.status, hook.url)
      webhookApi.saveWebhook(hook)
    }
  }
}
