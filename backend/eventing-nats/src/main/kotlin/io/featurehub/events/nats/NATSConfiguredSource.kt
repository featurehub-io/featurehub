package io.featurehub.events.nats

import io.featurehub.events.CloudEventConfig
import io.featurehub.events.CloudEventConfigDiscovery
import io.featurehub.events.CloudEventConfigDiscoveryProcessor
import io.featurehub.events.CloudEventPublisherConfig
import io.featurehub.events.CloudEventSubscriberConfig
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class NATSConfiguredSource @Inject constructor(private val ceConfigDiscovery: CloudEventConfigDiscovery,
                                               private val natsConnection: NATSSource
) : LifecycleStarted, LifecycleShutdown, CloudEventConfigDiscoveryProcessor {
  private val subscribers = mutableListOf<NatsListener>()
  private val publishers = mutableListOf<NatsCloudEventsPublisher>()

  override fun started() {
    ceConfigDiscovery.discover("nats", this)
  }

  override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
    if (publisher.channelNames.isEmpty()) {
      log.debug("nats: attempting to configure publisher {} channel {} and missing topic name", publisher.name, config.name)
      return
    }

    publisher.channelNames.forEach { channelName ->
      val natsPublisher = natsConnection.createPublisher(channelName)

      config.registerPublisher(publisher, channelName, true) { msg -> natsPublisher.publish(msg) }

      publishers.add(natsPublisher)
    }
  }

  override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
    if (subscriber.channelNames.isEmpty()) {
      log.error("NATS: attempting to configure subscriber {} channel {} and missing topic name", subscriber.name, config.name)
      return
    }

    subscriber.channelNames.forEach { channelName ->
      val listener = if (subscriber.broadcast) {
        log.trace("nats: creating topic listener for {}", channelName)
        natsConnection.createTopicListener(channelName, subscriber.handler )
      } else {
        val queue = subscriber.prefix ?: config.name
        log.trace("nats: creating queue listener for {} : {}", channelName, queue)
        natsConnection.createQueueListener(channelName, queue, subscriber.handler )
      }

      subscribers.add(listener)
    }
  }

  override fun shutdown() {
    subscribers.forEach { it.close() }
    subscribers.clear()
    publishers.clear()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(NATSConfiguredSource::class.java)
  }
}
