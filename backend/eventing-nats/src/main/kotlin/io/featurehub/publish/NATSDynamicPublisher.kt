package io.featurehub.publish

import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventPublisherRegistry
import jakarta.inject.Inject

/**
 * The NATS Dynamic Publisher will respond to a request for a dynamic publication channel and create it on the
 * fly and register it with the Cloud Events Publishing registry for a specific type. It will compress by default
 * but does not need to.
 */
class NATSDynamicPublisher @Inject constructor(
  private val nats: NATSSource,
  dynamicPublisher: CloudEventDynamicPublisherRegistry,
  private val publisherRegistry: CloudEventPublisher
) {
  init {
//    dynamicPublisher.registerDymamicPublisherProvider(listOf("nats://"), this::registerType)
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
