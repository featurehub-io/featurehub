package io.featurehub.messaging.publisher.nats

import cd.connect.app.config.ConfigKey
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.FeatureMessagingMetrics
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject


class NatsFeatureMessagingPublisher @Inject constructor(
  natsSource: NATSSource, cloudEventsPublisher: CloudEventPublisher
)  {
  @ConfigKey("cloudevents.messaging.nats.channel-name")
  protected var messagingChannelName: String? = "featurehub/messaging-events"

  init {
    val publisher = natsSource.createPublisher(messagingChannelName!!)

    cloudEventsPublisher.registerForPublishing(
      FeatureMessagingUpdate.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(FeatureMessagingMetrics.messagingPublishFailureCounter, FeatureMessagingMetrics.messagingPublishHistogram),
      true, publisher::publish
    )
  }
}


