package io.featurehub.mr.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.mr.events.common.CloudEventsDachaChannel
import io.featurehub.mr.events.common.CloudEventsEdgeChannel
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject


class NatsCloudEventsEdgeChannel @Inject constructor(nats: NATSSource) : CloudEventsEdgeChannel {
  @ConfigKey("cloudevents.edge.channel-name")
  private var edgeChannelName: String? = "featurehub/edge-channel"

  private val natsChannel : NatsCloudEventsPublisher

  init {
    DeclaredConfigResolver.resolve(this)

    natsChannel = nats.createPublisher(edgeChannelName!!)
  }

  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publishEvent(event: CloudEvent) {
    natsChannel.publish(event)
  }
}

class NatsCloudEventsDachaChannel @Inject constructor(nats: NATSSource) : CloudEventsDachaChannel {
  @ConfigKey("cloudevents.dacha2.channel-name")
  private var dachaChannelName: String? = "featurehub/dacha2-channel"

  private val natsChannel : NatsCloudEventsPublisher

  init {
    DeclaredConfigResolver.resolve(this)

    natsChannel = nats.createPublisher(dachaChannelName!!)
  }

  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publishEvent(event: CloudEvent) {
    natsChannel.publish(event)
  }
}

