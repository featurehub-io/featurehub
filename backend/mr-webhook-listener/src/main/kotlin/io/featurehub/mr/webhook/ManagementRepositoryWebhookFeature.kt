package io.featurehub.mr.webhook

import io.featurehub.db.api.WebhookApi
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.webhook.events.WebhookEnvironmentResult
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManagementRepositoryWebhookFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    LifecycleListeners.starter(WebhookListeners::class.java, context)

    return true
  }
}

@LifecyclePriority(priority = 15)
class WebhookListeners @Inject constructor(cloudEventsRegistry: CloudEventReceiverRegistry, webhookApi: WebhookApi): LifecycleListener {
  private val log: Logger = LoggerFactory.getLogger(WebhookListeners::class.java)
  init {
    cloudEventsRegistry.listen(WebhookEnvironmentResult::class.java) { hook, _ ->
      log.debug("webhook: {} >> {}", hook.status, hook.url)
      webhookApi.saveWebhook(hook)
    }
  }
}
