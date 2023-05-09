package io.featurehub.events.nats

import io.cloudevents.CloudEvent
import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.publish.NATSSource
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.Subscription
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsCloudEventTopicListener constructor(
  private val natsSource: NATSSource,
  private val subject: String,
  private val handler: (event: CloudEvent) -> Unit) : NatsListener {
  private val subscription: Subscription
  private val dispatcher: Dispatcher = natsSource.connection.createDispatcher()

  private val log: Logger = LoggerFactory.getLogger(NatsCloudEventTopicListener::class.java)

  init {
    subscription = dispatcher.subscribe(subject, this::process)

    log.info("nats: listening on topic {}", subject)
  }

  override fun close() {
    log.trace("NATS connection closed for subject {}", subject)
    natsSource.connection.closeDispatcher(dispatcher)
  }

  fun process(msg: Message) {
    try {
      handler(NatsMessageFactory.createReader(msg).toEvent())
    } catch (e: Exception) {
      log.error("Unable to process cloud event", e)
    }
  }
}
