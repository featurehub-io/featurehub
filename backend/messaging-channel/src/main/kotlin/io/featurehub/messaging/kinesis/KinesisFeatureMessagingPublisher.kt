package io.featurehub.messaging.kinesis

import cd.connect.app.config.ConfigKey
import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.kinesis.KinesisCloudEventsPublisher
import io.featurehub.events.kinesis.KinesisFactory
import io.featurehub.messaging.FeatureMessagingMetrics
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

open class KinesisFeatureMessageBase {
  val streamName: String
  val randomisePartitionKey: Boolean

  init {
    streamName = FallbackPropertyConfig.getConfig("cloudevents.mr-messaging.kinesis.stream-name", "featurehub-messaging-stream")
    randomisePartitionKey = FallbackPropertyConfig.getConfig("cloudevents.mr-messaging.kinesis.randomise-partition-key", "false") == "true"
  }
}

class KinesisFeatureMessagingPublisher @Inject constructor(
  kinesisFactory: KinesisFactory,
  cloudEventsPublisher: CloudEventPublisher
) : KinesisFeatureMessageBase() {
  private var publisher: KinesisCloudEventsPublisher
  private val uniqueAppKinesisPartitionId = UUID.randomUUID().toString()
  private val log: Logger = LoggerFactory.getLogger(KinesisFeatureMessagingPublisher::class.java)

  init {
    publisher = kinesisFactory.makePublisher(streamName)
    cloudEventsPublisher.registerForPublishing(
      FeatureMessagingUpdate.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(FeatureMessagingMetrics.messagingPublishFailureCounter, FeatureMessagingMetrics.messagingPublishHistogram),
      false, this::publishEvent
    )
  }

  private fun partitionKey(): String {
    return if (randomisePartitionKey) {
      UUID.randomUUID().toString()
    } else {
      uniqueAppKinesisPartitionId
    }
  }

  private fun publishEvent(event: CloudEvent) {
    val partitionKey = partitionKey()
    log.trace("publishing event {} with partition key {}", event, partitionKey)
    publisher.publish(event, partitionKey )
  }
}

class KinesisFeatureMessagingListener @Inject constructor(kinesisFactory: KinesisFactory, registry: CloudEventReceiverRegistry) : KinesisFeatureMessageBase() {
  init {
    kinesisFactory.makeSubscriber("webhook", streamName, { registry.process(it) })
  }
}
