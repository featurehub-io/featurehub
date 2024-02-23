package io.featurehub.events.pubsub

import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventDynamicDeliveryDetails
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import java.util.concurrent.ConcurrentHashMap

/**
 * Dynamically creates publishers for pubsub if required.
 */
@LifecyclePriority(priority = 5)
class PubsubDynamicPublisher @Inject constructor(private val pubSubFactory: PubSubFactory,
                                                 dynamicPublisher: CloudEventDynamicPublisherRegistry,

) : LifecycleListener {
  val publishers: ConcurrentHashMap<String, PubSubPublisher> = ConcurrentHashMap()

  init {
    dynamicPublisher.registerDynamicPublisherProvider(listOf("pubsub://"), this::publish)
  }

  fun publish(
    config: CloudEventDynamicDeliveryDetails,
    ce: CloudEvent, destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    val channel = publishers.computeIfAbsent(destSuffix) {
      pubSubFactory.makePublisher(destSuffix)
    }

    val timer = metric.perf.startTimer()
    try {
      channel.publish(ce)
    } catch (e: Exception) {
      metric.failures.inc()
    } finally {
      timer.observeDuration()
    }
  }
}
