package io.featurehub.events.nats

import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject

/**
 * The NATS Dynamic Publisher will respond to a request for a dynamic publication channel and create it on the
 * fly and register it with the Cloud Events Publishing registry for a specific type. It will compress by default
 * but does not need to.
 */
@LifecyclePriority(priority = 5)
class NATSDynamicPublisher @Inject constructor(
  private val nats: NATSSource,
  dynamicPublisher: CloudEventDynamicPublisherRegistry,
  private val publisherRegistry: CloudEventPublisherRegistry
): LifecycleListener {
  init {
    dynamicPublisher.registerDymamicPublisherProvider(listOf("nats://"), this::registerType)
  }

  fun registerType(
    params: Map<String, String>,
    cloudEventType: String,
    destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    val channel = nats.createPublisher(destSuffix)

    publisherRegistry.registerForPublishing(
      cloudEventType,
      metric,
      params["compress"]?.lowercase() == "false",
      channel::publish
    )
  }
}
