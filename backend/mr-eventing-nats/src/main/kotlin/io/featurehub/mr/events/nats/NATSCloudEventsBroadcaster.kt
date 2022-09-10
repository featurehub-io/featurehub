package io.featurehub.mr.events.nats

import io.cloudevents.CloudEvent
import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.mr.events.common.CloudEventBroadcasterWriter
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject

class NATSCloudEventsBroadcaster @Inject constructor(private val nats: NATSSource) : CloudEventBroadcasterWriter {
  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publish(event: CloudEvent) {
    nats.connection.publish(NatsMessageFactory.createWriter().writeBinary(event))
  }

}
