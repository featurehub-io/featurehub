package io.featurehub.edge.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.edge.events.CloudEventsEdgePublisher
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject

class NatsFeatureUpdatePublisher @Inject constructor(
  natsSource: NATSSource,
) : CloudEventsEdgePublisher {
  private val publisher: NatsCloudEventsPublisher

  @ConfigKey("cloudevents.edge-mr.nats.channel-name")
  private val updatesSubject: String = "featurehub/mr-updates-queue"

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = natsSource.createPublisher(updatesSubject!!)
  }

  override fun encodeAsJson(): Boolean {
    return false
  }

  override fun publish(event: CloudEvent) {
    publisher.publish(event)
  }
}
