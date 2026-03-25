package io.featurehub.events.kafka

import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventDynamicDeliveryDetails
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import java.util.concurrent.ConcurrentHashMap

/**
 * The Kafka Dynamic Publisher responds to requests for a dynamic publication channel with a `kafka://` prefix
 * and creates a publisher on the fly, caching it by topic name.
 */
@LifecyclePriority(priority = 5)
class KafkaDynamicPublisher @Inject constructor(
  private val kafkaFactory: KafkaFactory,
  dynamicPublisher: CloudEventDynamicPublisherRegistry
) : LifecycleListener {
  val publishers: ConcurrentHashMap<String, KafkaCloudEventsPublisher> = ConcurrentHashMap()

  init {
    dynamicPublisher.registerDynamicPublisherProvider(listOf("kafka://"), this::publish)
  }

  fun publish(
    config: CloudEventDynamicDeliveryDetails,
    ce: CloudEvent,
    destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    val publisher = publishers.computeIfAbsent(destSuffix) { kafkaFactory.makePublisher(destSuffix) }

    val timer = metric.perf.startTimer()
    try {
      publisher.publish(ce)
    } catch (e: Exception) {
      metric.failures.inc()
    } finally {
      timer.observeDuration()
    }
  }
}
