package io.featurehub.events.kinesis

import io.featurehub.events.CloudEventConfig
import io.featurehub.events.CloudEventConfigDiscovery
import io.featurehub.events.CloudEventConfigDiscoveryProcessor
import io.featurehub.events.CloudEventPublisherConfig
import io.featurehub.events.CloudEventSubscriberConfig
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleStarted
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class KinesisConfiguredSource @Inject constructor(
  private val ceConfigDiscovery: CloudEventConfigDiscovery,
  private val kinesisFactory: KinesisFactory
) : LifecycleStarted, CloudEventConfigDiscoveryProcessor {
  private val log: Logger = LoggerFactory.getLogger(KinesisConfiguredSource::class.java)

  override fun started() {
    ceConfigDiscovery.discover("kinesis", this)
  }

  /**
   * Kinesis is a bit different because we want control over randomisation for partitions. Let's leave this for now
   * however as kinesis is unknown whether it even works.
   */
  override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
    if (publisher.channelNames.isEmpty()) {
      log.debug(
        "kinesis: attempting to configure publisher {} channel {} and missing topic name",
        publisher.name,
        config.name
      )
      return
    }

    publisher.channelNames.forEach { channelName ->
      val kinesisPublisher = kinesisFactory.makePublisher(channelName)

      config.registerPublisher(publisher, channelName, true) { msg -> kinesisPublisher.publish(msg, publisher.name) }
    }
  }

  override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
    if (subscriber.channelNames.isEmpty()) {
      log.error("kinesis channel {} has no subscribers for config {}", subscriber.name, config.name)
      return
    }

    subscriber.channelNames.forEach { channelName ->
      val subscriberName =
        if (subscriber.broadcast) "${subscriber.prefix}-${UUID.randomUUID().toString()}" else subscriber.prefix
      kinesisFactory.makeSubscriber(subscriberName, channelName, subscriber.handler)
    }
  }
}
