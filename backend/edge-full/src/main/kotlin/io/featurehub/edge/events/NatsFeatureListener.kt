package io.featurehub.edge.events

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.events.nats.NatsListener
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.publish.NATSSource
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsFeatureListener @Inject constructor(private val controller: StreamingFeatureController, nats: NATSSource) {
  @ConfigKey("cloudevents.mr-edge.nats.channel-name")
  private var edgeChannelName: String? = "featurehub/mr-edge-channel"
  private val listener: NatsListener

  private val log: Logger = LoggerFactory.getLogger(NatsFeatureListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    listener = nats.createTopicListener(edgeChannelName!!) {event ->
      if (event.subject == PublishFeatureValues.CLOUD_EVENT_SUBJECT && event.type == PublishFeatureValues.CLOUD_EVENT_TYPE) {
        CacheJsonMapper.fromEventData(event, PublishFeatureValues::class.java)?.let {
          controller.updateFeatures(it)
        } ?: log.error("failed to decode event {}", event)
      }
    }
  }

  @PreDestroy
  fun close() {
    listener.close()
  }
}
