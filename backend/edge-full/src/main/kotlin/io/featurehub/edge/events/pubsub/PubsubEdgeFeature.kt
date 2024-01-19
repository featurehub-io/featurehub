package io.featurehub.edge.events.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.edge.events.EdgeSubscriber
import io.featurehub.edge.events.StreamingEventPublisher
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.webhook.common.WebhookCommonFeature
import io.featurehub.webhook.events.WebhookEnvironmentResult
import io.features.webhooks.features.WebhookFeature
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PubsubEdgeFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    if (!GoogleEventFeature.isEnabled()) return false

    LifecycleListeners.starter(PubsubFeaturesListener::class.java, context)
    LifecycleListeners.starter(PubsubFeatureUpdatePublisher::class.java, context)

    return true
  }
}

@LifecyclePriority(priority = 12)
class PubsubFeaturesListener @Inject constructor(
  private val controller: EdgeSubscriber,
  pubsubFactory: PubSubFactory) : LifecycleListener {
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

@LifecyclePriority(priority = 12)
class PubsubFeatureUpdatePublisher @Inject constructor(pubsubFactory: PubSubFactory, cloudEventPublisher: CloudEventPublisher) : LifecycleListener {
  @ConfigKey("cloudevents.edge-mr.pubsub.topic-name")
  private val updateChannelName: String = "featurehub-edge-updates"

  init {
    DeclaredConfigResolver.resolve(this)

    val publisher = pubsubFactory.makePublisher(updateChannelName)

    cloudEventPublisher.registerForPublishing(
      StreamedFeatureUpdate.CLOUD_EVENT_TYPE,
      StreamingEventPublisher.channelMetric, true, publisher::publish)

    if (WebhookFeature.enabled) {
      cloudEventPublisher.registerForPublishing(
        WebhookEnvironmentResult.CLOUD_EVENT_TYPE,
        WebhookCommonFeature.channelMetric, true
      ) {
        publisher.publish(it)
      }
    }
  }
}
