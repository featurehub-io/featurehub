package io.featurehub.edge.stats

import io.cloudevents.CloudEvent
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class NATSStatPublisher @Inject constructor(nats : NATSSource) : CloudEventStatPublisher {
  private val publisher: NatsCloudEventsPublisher
  init {
    publisher = nats.createPublisher("featurehub/edge-stats")
  }

  override fun encodeAsJson(): Boolean = false

  override fun publish(event: CloudEvent) {
    publisher.publish(event)
  }
}
