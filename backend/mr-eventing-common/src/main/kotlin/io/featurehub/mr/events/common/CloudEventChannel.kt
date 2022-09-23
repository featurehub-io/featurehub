package io.featurehub.mr.events.common

import io.cloudevents.CloudEvent

interface CloudEventChannel {
  fun encodePureJson(): Boolean
  fun publishEvent(event: CloudEvent)
}

interface CloudEventsDachaChannel : CloudEventChannel {
  fun dacha2Enabled(): Boolean
}

interface CloudEventsEdgeChannel : CloudEventChannel
