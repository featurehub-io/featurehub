package io.featurehub.events.pubsub

import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisherRegistry
import jakarta.inject.Inject

/**
 * Dynamically creates publishers for pubsub if required.
 */
class PubsubDynamicPublisher @Inject constructor(private val pubSubFactory: PubSubFactory, dynamicPublisher: CloudEventDynamicPublisherRegistry,
                                                 private val publisherRegistry: CloudEventPublisherRegistry
) {
  init {
    dynamicPublisher.registerDymamicPublisherProvider(listOf("pubsub://"), this::registerType)
  }

  fun registerType(
    params: Map<String, String>,
    cloudEventType: String,
    destination: String,
    destSuffix: String,
    metric: CloudEventChannelMetric
  ) {
    val channel = pubSubFactory.makePublisher(destSuffix)

    publisherRegistry.registerForPublishing(
      cloudEventType,
      metric,
      params["compress"]?.lowercase() == "false",
      channel::publish
    )
  }
}
