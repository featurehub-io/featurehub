package io.featurehub.edge.events.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.edge.events.CloudEventsEdgePublisher
import io.featurehub.edge.events.EdgeSubscriber
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.events.pubsub.PubSubPublisher
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PubsubEdgeFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!GoogleEventFeature.isEnabled()) return false

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(PubsubFeaturesListener::class.java)
          .to(PubsubFeaturesListener::class.java).`in`(Immediate::class.java)
        bind(PubsubFeatureUpdatePublisher::class.java)
          .to(CloudEventsEdgePublisher::class.java).`in`(Singleton::class.java)
      }
    })

    return true
  }
}

class PubsubFeaturesListener @Inject constructor(
  private val controller: EdgeSubscriber,
  pubsubFactory: PubSubFactory) {
  @ConfigKey("cloudevents.mr-edge.pubsub.topic-name")
  private var edgeTopicName: String? = "featurehub-mr-edge"
  @ConfigKey("cloudevents.mr-edge.pubsub.subscription-prefix")
  var subscriptionPrefix: String? = "featurehub-edge-listener"

  private val log: Logger = LoggerFactory.getLogger(PubsubFeaturesListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    pubsubFactory.makeUniqueSubscriber(edgeTopicName!!, subscriptionPrefix!!) {
      try {
        controller.process(it)
      } catch (e: Exception) {
        log.error("Unable to process feature update", e)
      }
      true
    }
  }
}

class PubsubFeatureUpdatePublisher @Inject constructor(pubsubFactory: PubSubFactory) : CloudEventsEdgePublisher {
  @ConfigKey("cloudevents.edge-mr.pubsub.channel-name")
  private val updateChannelName: String = "featurehub-edge-updates"
  private var publisher: PubSubPublisher

  init {
    DeclaredConfigResolver.resolve(this)

    publisher = pubsubFactory.makePublisher(updateChannelName)
  }

  override fun encodeAsJson() = false

  override fun publish(event: CloudEvent) {
    publisher.publish(event)
  }
}
