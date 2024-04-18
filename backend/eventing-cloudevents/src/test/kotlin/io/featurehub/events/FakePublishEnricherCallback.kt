package io.featurehub.events

import cd.connect.cloudevents.TaggedCloudEvent

class FakePublishEnricherCallback(val proc: CloudEventPublisherRegistryProcessor) {
  var trigger: Int = 0

  fun <T: TaggedCloudEvent> publish(data: T) {
    proc.publish(data) { evtBuilder ->
      trigger ++
      evtBuilder
    }
  }
}
