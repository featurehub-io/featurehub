package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.featurehub.events.pubsub.PubSubFactory
import io.featurehub.events.pubsub.PubSubPublisher
import jakarta.inject.Inject

class PubsubStatsPublisher @Inject constructor(pubSubFactory: PubSubFactory) : CloudEventStatPublisher {
  @ConfigKey("cloudevents.stats.pubsub.topic-name")
  var topicName: String? = "featurehub-stats"
  private var publisher: PubSubPublisher

  init {
    DeclaredConfigResolver.resolve(this)
    publisher = pubSubFactory.makePublisher(topicName!!)
  }

  override fun encodeAsJson(): Boolean {
    return false
  }

  override fun publish(event: CloudEvent) {
    publisher.publish(event)
  }
}
