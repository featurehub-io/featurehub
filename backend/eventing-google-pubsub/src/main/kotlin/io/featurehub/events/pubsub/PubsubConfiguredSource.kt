package io.featurehub.events.pubsub

import io.cloudevents.CloudEvent
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

@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class PubsubConfiguredSource @Inject constructor(private val pubSubFactory: PubSubFactory, private val ceConfigDiscovery: CloudEventConfigDiscovery) : LifecycleStarted,
  CloudEventConfigDiscoveryProcessor {
  override fun started() {
    ceConfigDiscovery.discover("pubsub", this)
  }

  /**
   * Kinesis is a bit different because we want control over randomisation for partitions. Let's leave this for now
   * however as kinesis is unknown whether it even works.
   */
  override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
    if (publisher.channelNames.isEmpty()) {
      log.debug(
        "pubsub: attempting to configure publisher {} channel {} and missing topic name",
        publisher.name,
        config.name
      )
      return
    }

    publisher.channelNames.forEach { channelName ->
      val pubsubPublisher = pubSubFactory.makePublisher(channelName)

      config.registerPublisher(publisher, channelName, true) { msg -> pubsubPublisher.publish(msg) }
    }
  }

  override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
    if (subscriber.channelNames.isEmpty()) {
      log.error("pubsub channel {} has no subscribers for config {}", subscriber.name, config.name)
      return
    }

    val handler = { msg: CloudEvent ->
      subscriber.handler(msg)
      true
    }

    subscriber.channelNames.forEach { channelName ->
      if (subscriber.broadcast) {
        pubSubFactory.makeUniqueSubscriber(channelName, subscriber.prefix, handler)
      } else {
        pubSubFactory.makeSubscriber(channelName, handler)
      }
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(PubsubConfiguredSource::class.java)
  }
}
