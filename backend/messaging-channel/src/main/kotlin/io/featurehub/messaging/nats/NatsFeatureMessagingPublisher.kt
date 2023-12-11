package io.featurehub.messaging.nats

import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.messaging.FeatureMessagingMetrics
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.publish.NATSSource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject

open class NatsFeatureMessagingBase {
  protected val messagingChannelName: String

  init {
    messagingChannelName = FallbackPropertyConfig.getConfig("cloudevents.mr-messaging.nats.channel-name", "featurehub/messaging-events")
  }
}

class NatsFeatureMessagingPublisher @Inject constructor(
  natsSource: NATSSource, cloudEventsPublisher: CloudEventPublisher
) : NatsFeatureMessagingBase()  {

  init {
    val publisher = natsSource.createPublisher(messagingChannelName)

    cloudEventsPublisher.registerForPublishing(
      FeatureMessagingUpdate.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(FeatureMessagingMetrics.messagingPublishFailureCounter, FeatureMessagingMetrics.messagingPublishHistogram),
      true, publisher::publish
    )
  }
}

class NatsFeatureMessagingListener @Inject constructor(natsSource: NATSSource, registry: CloudEventReceiverRegistry): NatsFeatureMessagingBase() {
  init {
    val listener = natsSource.createQueueListener(messagingChannelName, "webhook") {
      registry.process(it)
    }
  }
}


