package io.featurehub.events

import io.cloudevents.CloudEvent

class FakePublishCallback(private val cloudEventType: String, metric: CloudEventChannelMetric, compress: Boolean, proc: CloudEventPublisherRegistryProcessor) {
  var event: CloudEvent? = null

  init {
    proc.registerForPublishing(cloudEventType, metric, compress, this::publish)
  }

  private fun publish(cloudEvent: CloudEvent) {
    event = cloudEvent
  }

  // if it is a val, Groovy evaluates it too early
  fun text() : String? { return event?.data?.toBytes()?.toString(Charsets.UTF_8) }
}
