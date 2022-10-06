package io.featurehub.mr.events.dacha2.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.events.pubsub.PubSubPublisher
import io.featurehub.mr.events.common.CloudEventsDachaChannel
import io.featurehub.mr.events.common.CloudEventsEdgeChannel
import io.featurehub.mr.events.common.Dacha2Config
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class PubsubMRFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!GoogleEventFeature.isEnabled()) return false

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(PubsubCloudEventsEdgeChannel::class.java).to(CloudEventsEdgeChannel::class.java)
          .`in`(Singleton::class.java)
        bind(PubsubCloudEventsDachaChannel::class.java).to(CloudEventsDachaChannel::class.java)
          .`in`(Singleton::class.java)
      }
    })

    return true
  }
}

class PubsubCloudEventsEdgeChannel @Inject constructor(pubSubFactory: PubSubFactory) : CloudEventsEdgeChannel {
  @ConfigKey("cloudevents.mr-edge.pubsub.topic-name")
  private var edgeChannelName: String? = "featurehub-mr-edge"

  private val publisher: PubSubPublisher

  init {
    DeclaredConfigResolver.resolve(this)
    publisher = pubSubFactory.makePublisher(edgeChannelName!!)
  }

  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publishEvent(event: CloudEvent) {
    publisher.publish(event)
  }

}

class PubsubCloudEventsDachaChannel @Inject constructor(pubSubFactory: PubSubFactory) : CloudEventsDachaChannel {
  @ConfigKey("cloudevents.mr-dacha2.pubsub.topic-name")
  private var dachaChannelName: String? = "featurehub-mr-dacha2"

  private val publisher: PubSubPublisher

  init {
    DeclaredConfigResolver.resolve(this)
    publisher = pubSubFactory.makePublisher(dachaChannelName!!)
  }

  override fun dacha2Enabled(): Boolean {
    return Dacha2Config.isDacha2Enabled()
  }

  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publishEvent(event: CloudEvent) {
    publisher.publish(event)
  }
}
