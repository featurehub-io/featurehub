package io.featurehub.events.local

import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventConfig
import io.featurehub.events.CloudEventConfigDiscovery
import io.featurehub.events.CloudEventConfigDiscoveryProcessor
import io.featurehub.events.CloudEventPublisherConfig
import io.featurehub.events.CloudEventSubscriberConfig
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.utils.ExecutorSupplier
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

/**
 * A single in-process subscriber. Each one has its own single threaded executor so events are delivered to it
 * serially and in the order they were handed over, the same way a NATS subscription delivers on a single dispatcher.
 */
class LocalSubscriber(
  val channel: String,
  val subscriber: CloudEventSubscriberConfig,
  private val executor: ExecutorService
) {
  fun deliver(ce: CloudEvent) {
    try {
      executor.submit {
        try {
          // the handler already applies cloudEventsInclude and routes into the correct ceRegistry
          subscriber.handler(ce)
        } catch (e: Exception) {
          log.error("local: subscriber {} on channel {} failed to process {}", subscriber.name, channel, ce.type, e)
        }
      }
    } catch (e: RejectedExecutionException) {
      log.debug("local: subscriber {} is shut down, dropping {}", subscriber.name, ce.type)
    }
  }

  fun shutdown() {
    executor.shutdown()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(LocalSubscriber::class.java)
  }
}

/**
 * The in-process ("local") eventing platform. It is a loopback: every event published on a channel is delivered
 * straight to the subscribers of the same channel in this process. It is driven by the same cloud-events.yaml
 * discovery and tags as NATS, so the routing is identical to a NATS round trip from a single instance.
 *
 * Only used when no external eventing platform is enabled and cloudevents.local.enabled=true.
 */
@LifecyclePriority(priority = LifecyclePriority.APPLICATION_PRIORITY_END)
class LocalConfiguredSource @Inject constructor(
  private val ceConfigDiscovery: CloudEventConfigDiscovery,
  private val executorSupplier: ExecutorSupplier
) : LifecycleStarted, LifecycleShutdown, CloudEventConfigDiscoveryProcessor {
  private val subscribersByChannel = ConcurrentHashMap<String, CopyOnWriteArrayList<LocalSubscriber>>()
  private val publishedChannels = mutableSetOf<String>()

  override fun started() {
    ceConfigDiscovery.discover(PLATFORM, this)

    publishedChannels.filter { subscribersByChannel[it].isNullOrEmpty() }.forEach { channel ->
      log.info("local: channel {} has publishers but no in-process subscribers; events will be dropped", channel)
    }
  }

  override fun processPublisher(publisher: CloudEventPublisherConfig, config: CloudEventConfig) {
    val channel = config.name
    publishedChannels.add(channel)

    // subscribers are resolved at publish time, discovery processes all publishers before any subscribers.
    // we use the logical channel name, the transport channel names are irrelevant in-process. There is no network
    // so there is no point compressing.
    config.registerPublisher(publisher, channel, false) { ce ->
      val subs = subscribersByChannel[channel]
      if (subs.isNullOrEmpty()) {
        log.debug("local: no in-process subscriber for {} on {}", ce.type, channel)
      } else {
        subs.forEach { it.deliver(ce) }
      }
    }
  }

  override fun processSubscriber(subscriber: CloudEventSubscriberConfig, config: CloudEventConfig) {
    log.trace("local: subscriber {} listening on channel {}", subscriber.name, config.name)
    subscribersByChannel.computeIfAbsent(config.name) { CopyOnWriteArrayList() }
      .add(LocalSubscriber(config.name, subscriber, executorSupplier.executorService(1)))
  }

  override fun shutdown() {
    subscribersByChannel.values.forEach { subs -> subs.forEach { it.shutdown() } }
    subscribersByChannel.clear()
  }

  /**
   * for testing
   */
  fun subscribers(channel: String): List<LocalSubscriber> = subscribersByChannel[channel] ?: listOf()

  companion object {
    const val PLATFORM = "local"
    private val log: Logger = LoggerFactory.getLogger(LocalConfiguredSource::class.java)
  }
}
