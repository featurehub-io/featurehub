package io.featurehub.edge.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.edge.events.StreamingEventPublisher
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.publish.NATSSource
import io.featurehub.webhook.common.WebhookCommonFeature
import io.featurehub.webhook.events.WebhookEnvironmentResult
import io.features.webhooks.features.WebhookFeature
import jakarta.inject.Inject

@LifecyclePriority(priority = 12)
class NatsFeatureUpdatePublisher @Inject constructor(
  natsSource: NATSSource,
  cloudEventPublisher: CloudEventPublisherRegistry
) :LifecycleListener  {
  @ConfigKey("cloudevents.edge-mr.nats.channel-name")
  private val updatesSubject: String = "featurehub/mr-updates-queue"

  init {
    DeclaredConfigResolver.resolve(this)

    val publisher = natsSource.createPublisher(updatesSubject)

    cloudEventPublisher.registerForPublishing(StreamedFeatureUpdate.CLOUD_EVENT_TYPE,
      StreamingEventPublisher.channelMetric, true, publisher::publish)

    if (WebhookFeature.enabled) {
      cloudEventPublisher.registerForPublishing(WebhookEnvironmentResult.CLOUD_EVENT_TYPE,
        WebhookCommonFeature.channelMetric, true, publisher::publish)
    }
  }
}
