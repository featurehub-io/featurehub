package io.featurehub.dacha2.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.kinesis.KinesisFactory
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class KinesisDachaEventsListener @Inject constructor(kinesisFactory: KinesisFactory, eventListener: CloudEventReceiverRegistry) {
  @ConfigKey("cloudevents.mr-dacha2.kinesis.stream-name")
  var topicName: String? = "featurehub-mr-dacha2"

  private val log: Logger = LoggerFactory.getLogger(KinesisDachaEventsListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    kinesisFactory.makeSubscriber("dacha2-" + UUID.randomUUID().toString(), topicName!!) {event ->
      eventListener.process(event)
    }
  }
}
