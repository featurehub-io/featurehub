package io.featurehub.messaging.publisher.kinesis

import cd.connect.app.config.ConfigKey
import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.kinesis.KinesisCloudEventsPublisher
import io.featurehub.events.kinesis.KinesisFactory
import io.featurehub.messaging.FeatureMessagingMetrics
import io.featurehub.messaging.model.FeatureMessagingUpdate
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class KinesisFeatureMessagingPublisher @Inject constructor(
  kinesisFactory: KinesisFactory,
  cloudEventsPublisher: CloudEventPublisher
) {

  @ConfigKey("cloudevents.mr-messaging.kinesis.stream-name")
  private var streamName: String? = "featurehub-messaging-stream"

  @ConfigKey("cloudevents.mr-messaging.kinesis.randomise-partition-key")
  private var randomisePartitionKey: Boolean? = false

  private var publisher: KinesisCloudEventsPublisher
  private val uniqueAppKinesisPartitionId = UUID.randomUUID().toString()
  private val log: Logger = LoggerFactory.getLogger(KinesisFeatureMessagingPublisher::class.java)

  init {
    publisher = kinesisFactory.makePublisher(streamName!!)
    cloudEventsPublisher.registerForPublishing(
      FeatureMessagingUpdate.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(FeatureMessagingMetrics.messagingPublishFailureCounter, FeatureMessagingMetrics.messagingPublishHistogram),
      false, this::publishEvent
    )
  }

  private fun partitionKey(): String {
    return if (randomisePartitionKey == true) {
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
