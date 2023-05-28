package io.featurehub.messaging.publisher.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.messaging.FeatureMessagingMetrics
import io.featurehub.messaging.model.FeatureMessagingUpdate
import jakarta.inject.Inject
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class PubsubFeatureMessagingPublisher @Inject constructor(pubSubFactory: PubSubFactory, cloudEventsPublisher: CloudEventPublisher) {
  @ConfigKey("cloudevents.mr-messaging.pubsub.topic-name")
  private var messagingChannelName: String? = "featurehub-messaging-channel"

  init {
    DeclaredConfigResolver.resolve(this)
    val publisher = pubSubFactory.makePublisher(messagingChannelName!!)

    cloudEventsPublisher.registerForPublishing(
      FeatureMessagingUpdate.CLOUD_EVENT_TYPE,
      CloudEventChannelMetric(FeatureMessagingMetrics.messagingPublishFailureCounter, FeatureMessagingMetrics.messagingPublishHistogram),
      true, publisher::publish)
  }
}
