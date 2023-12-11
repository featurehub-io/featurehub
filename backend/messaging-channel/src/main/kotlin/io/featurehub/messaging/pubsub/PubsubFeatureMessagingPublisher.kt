package io.featurehub.messaging.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.messaging.FeatureMessagingMetrics
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

open class PubsubFeatureMessagingBase {
  var messagingChannelName: String

  init {
    messagingChannelName = FallbackPropertyConfig.getConfig("cloudevents.mr-messaging.pubsub.topic-name", "featurehub-messaging-channel")
  }
}

class PubsubFeatureMessagingPublisher @Inject constructor(pubSubFactory: PubSubFactory, cloudEventsPublisher: CloudEventPublisher) : PubsubFeatureMessagingBase() {
  init {
    val publisher = pubSubFactory.makePublisher(messagingChannelName)

    cloudEventsPublisher.registerForPublishing(
      FeatureMessagingUpdate.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(FeatureMessagingMetrics.messagingPublishFailureCounter, FeatureMessagingMetrics.messagingPublishHistogram),
      true, publisher::publish)
  }
}

class PubsubFeatureMessagingListener @Inject constructor(pubSubFactory: PubSubFactory, registry: CloudEventReceiverRegistry): PubsubFeatureMessagingBase() {
  init {
    pubSubFactory.makeSubscriber(messagingChannelName, {
      registry.process(it)
      true
    })
  }
}
