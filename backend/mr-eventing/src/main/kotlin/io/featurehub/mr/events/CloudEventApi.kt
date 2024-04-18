package io.featurehub.mr.events

interface CloudEventApi {
  fun save(cloudEvents: List<io.cloudevents.CloudEvent>)
}
