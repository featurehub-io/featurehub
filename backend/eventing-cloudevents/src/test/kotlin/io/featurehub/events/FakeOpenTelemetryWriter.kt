package io.featurehub.events

import io.cloudevents.CloudEvent

class FakeOpenTelemetryWriter : CloudEventsTelemetryReader {
  var fullTrigger: Int = 0
  var simpleTrigger: Int = 0

  override fun receive(
    subject: String,
    event: CloudEvent,
    metrics: CloudEventChannelMetric,
    process: (event: CloudEvent) -> Unit
  ) {
    fullTrigger ++
    process(event)
  }

  override fun receive(event: CloudEvent, process: (event: CloudEvent) -> Unit) {
    simpleTrigger ++
    process(event)
  }
}
