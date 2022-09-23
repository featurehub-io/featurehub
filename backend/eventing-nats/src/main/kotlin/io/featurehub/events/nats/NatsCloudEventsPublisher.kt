package io.featurehub.events.nats

import io.cloudevents.CloudEvent
import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.publish.NATSSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsCloudEventsPublisher constructor(private val natsSource: NATSSource, private val subject: String) {
  private val log: Logger = LoggerFactory.getLogger(NatsCloudEventsPublisher::class.java)

  init {
    log.info("nats: ready to publish on topic {}", subject)
  }

  fun publish(event: CloudEvent) {
    try {
      natsSource.connection.publish(
        NatsMessageFactory.createWriter(subject).writeBinary(event)
      )
    } catch (e: Exception) {
      log.error("Unable to publish event {}", event, e)
    }
  }
}
