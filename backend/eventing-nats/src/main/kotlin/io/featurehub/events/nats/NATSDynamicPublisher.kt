package io.featurehub.events.nats

import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventDynamicDeliveryDetails
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import java.util.concurrent.ConcurrentHashMap

/**
 * The NATS Dynamic Publisher will respond to a request for a dynamic publication channel and create it on the
 * fly and register it with the Cloud Events Publishing registry for a specific type. It will compress by default
 * but does not need to.
 */
@LifecyclePriority(priority = 5)
class NATSDynamicPublisher @Inject constructor(
  private val nats: NATSSource,
  dynamicPublisher: CloudEventDynamicPublisherRegistry,
): LifecycleListener {
  val publishers: ConcurrentHashMap<String, NatsCloudEventsPublisher> = ConcurrentHashMap()

  init {
    dynamicPublisher.registerDynamicPublisherProvider(listOf("nats://"), this::publish)
  }

  fun publish(
    config: CloudEventDynamicDeliveryDetails,
    ce: CloudEvent, destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    val channel = publishers.computeIfAbsent(destSuffix) { nats.createPublisher(destSuffix) }

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
