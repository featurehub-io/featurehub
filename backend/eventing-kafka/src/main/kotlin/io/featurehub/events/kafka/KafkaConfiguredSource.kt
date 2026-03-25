package io.featurehub.events.kafka

import io.featurehub.events.CloudEventConfig
import io.featurehub.events.CloudEventConfigDiscovery
import io.featurehub.events.CloudEventConfigDiscoveryProcessor
import io.featurehub.events.CloudEventPublisherConfig
import io.featurehub.events.CloudEventSubscriberConfig
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class KafkaConfiguredSource @Inject constructor(
  private val ceConfigDiscovery: CloudEventConfigDiscovery,
  private val kafkaFactory: KafkaFactory
) : LifecycleStarted, LifecycleShutdown, CloudEventConfigDiscoveryProcessor {
  private val log: Logger = LoggerFactory.getLogger(KafkaConfiguredSource::class.java)
  private val listeners = mutableListOf<KafkaListener>()

  override fun started() {
    ceConfigDiscovery.discover("kafka", this)
  }

  override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
    if (publisher.channelNames.isEmpty()) {
      log.debug("kafka: attempting to configure publisher {} channel {} and missing topic name", publisher.name, config.name)
      return
    }

    publisher.channelNames.forEach { topic ->
      val pub = kafkaFactory.makePublisher(topic)
      config.registerPublisher(publisher, topic, true) { msg -> pub.publish(msg) }
    }
  }

  override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
    if (subscriber.channelNames.isEmpty()) {
      log.error("kafka: attempting to configure subscriber {} channel {} and missing topic name", subscriber.name, config.name)
      return
    }

    subscriber.channelNames.forEach { topic ->
      val groupId = if (subscriber.broadcast)
        "${subscriber.prefix}-${UUID.randomUUID()}"
      else
        subscriber.prefix

      log.trace("kafka: creating {} subscriber '{}' on topic {}", if (subscriber.broadcast) "broadcast" else "queue", groupId, topic)
      listeners.add(kafkaFactory.makeSubscriber(groupId, topic, subscriber.handler))
    }
  }

  override fun shutdown() {
    listeners.forEach { it.close() }
    listeners.clear()
  }
}
