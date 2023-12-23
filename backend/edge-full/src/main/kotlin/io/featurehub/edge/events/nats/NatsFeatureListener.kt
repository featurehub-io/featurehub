package io.featurehub.edge.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.edge.events.EdgeSubscriber
import io.featurehub.events.nats.NatsListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@LifecyclePriority(priority = 12)
class NatsFeatureListener @Inject constructor(private val controller: EdgeSubscriber, nats: NATSSource) : LifecycleShutdown {
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

  override fun shutdown() {
    listener.close()
  }
}
