package io.featurehub.mr.events.dacha2.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.events.pubsub.PubSubPublisher
import io.featurehub.events.pubsub.PubSubSubscriber
import io.featurehub.mr.events.common.CloudEventsDachaChannel
import io.featurehub.mr.events.common.CloudEventsEdgeChannel
import io.featurehub.mr.events.common.Dacha2Config
import io.featurehub.mr.events.common.listeners.CloudEventListener
import io.featurehub.mr.events.nats.NatsMRCloudEventsQueueUpdateListener
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PubsubMRFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (GoogleEventFeature.isEnabled()) {
      context.register(object : AbstractBinder() {
        override fun configure() {
          bind(PubsubCloudEventsEdgeChannel::class.java).to(CloudEventsEdgeChannel::class.java)
            .`in`(Singleton::class.java)
          bind(PubsubCloudEventsDachaChannel::class.java).to(CloudEventsDachaChannel::class.java)
            .`in`(Singleton::class.java)

          // always listen for Edge updates in Cloud Events format
          bind(PubsubMRCloudEventsQueueUpdateListener::class.java).to(PubsubMRCloudEventsQueueUpdateListener::class.java)
            .`in`(
              Immediate::class.java
            )
        }
      })

      return true
    }

    return false
  }
}

class PubsubCloudEventsEdgeChannel @Inject constructor(pubSubFactory: PubSubFactory) : CloudEventsEdgeChannel {
  @ConfigKey("cloudevents.mr-edge.pubsub.channel-name")
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
  @ConfigKey("cloudevents.mr-dacha2.pubsub.channel-name")
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

// listens for updates from Edge
class PubsubMRCloudEventsQueueUpdateListener @Inject constructor(pubSubFactory: PubSubFactory,
  private val updateListener: CloudEventListener) {
  private val featureUpdaterSub: PubSubSubscriber
  private val log: Logger = LoggerFactory.getLogger(NatsMRCloudEventsQueueUpdateListener::class.java)
  @ConfigKey("cloudevents.edge-mr.pubsub.channel-name")
  private val updatesSubscription: String = "featurehub-edge-updates-mr-sub"

  init {
    DeclaredConfigResolver.resolve(this)

    log.info("Listening for MR updates on {}", updatesSubscription)

    featureUpdaterSub = pubSubFactory.makeSubscriber(updatesSubscription) { event ->
      updateListener.process(event)
      true
    }
  }
}
