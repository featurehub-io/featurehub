package io.featurehub.edge.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.edge.events.EdgeSubscriber
import io.featurehub.events.nats.NatsListener
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.publish.NATSSource
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsFeatureListener @Inject constructor(private val controller: EdgeSubscriber, nats: NATSSource) {
  @ConfigKey("cloudevents.mr-edge.nats.channel-name")
  private var edgeChannelName: String? = "featurehub/mr-edge-channel"
  private val listener: NatsListener

  private val log: Logger = LoggerFactory.getLogger(NatsFeatureListener::class.java)

  init {
      DeclaredConfigResolver.resolve(this)

    listener = nats.createTopicListener(edgeChannelName!!) { event ->
      controller.process(event)
    }
  }

  @PreDestroy
  fun close() {
    listener.close()
  }
}
