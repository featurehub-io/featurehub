package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class NATSStatPublisher @Inject constructor(nats : NATSSource) : CloudEventStatPublisher {
  @ConfigKey("cloudevents.stats.nats.channel-name")
  var subject: String? = "featurehub/edge-stats"

  private val publisher: NatsCloudEventsPublisher
  init {
    DeclaredConfigResolver.resolve(this)
    publisher = nats.createPublisher(subject!!)
  }

  override fun encodeAsJson(): Boolean = false

  override fun publish(event: CloudEvent) {
    publisher.publish(event)
  }
}
