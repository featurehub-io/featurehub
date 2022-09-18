package io.featurehub.mr.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.db.publish.nats.NatsDachaEventingFeature
import io.featurehub.events.nats.NatsCloudEventsPublisher
import io.featurehub.mr.events.common.CloudEventsDachaChannel
import io.featurehub.mr.events.common.CloudEventsEdgeChannel
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject


/**
 * This broadcasts events specifically for the consumption of Edge (which is just feature updates)
 */
class NatsCloudEventsEdgeChannel @Inject constructor(nats: NATSSource) : CloudEventsEdgeChannel {
  @ConfigKey("cloudevents.mr-edge.nats.channel-name")
  private var edgeChannelName: String? = "featurehub/mr-edge-channel"

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
  @ConfigKey("cloudevents.mr-dacha2.nats.channel-name")
  private var dachaChannelName: String? = "featurehub/mr-dacha2-channel"

  private val natsChannel : NatsCloudEventsPublisher
  private val isDacha2Enabled: Boolean

  init {
    DeclaredConfigResolver.resolve(this)
    isDacha2Enabled = NatsDachaEventingFeature.isDacha2Enabled()
    natsChannel = nats.createPublisher(dachaChannelName!!)
  }

  override fun dacha2Enabled(): Boolean = isDacha2Enabled

  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publishEvent(event: CloudEvent) {
    natsChannel.publish(event)
  }
}

