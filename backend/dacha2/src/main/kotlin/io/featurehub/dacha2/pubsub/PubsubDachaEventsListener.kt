package io.featurehub.dacha2.pubsub

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.pubsub.PubSubFactory
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PubsubDachaEventsListener @Inject constructor(pubSubFactory: PubSubFactory, eventListener: CloudEventReceiverRegistry) {
  @ConfigKey("cloudevents.mr-dacha2.pubsub.topic-name")
  var topicName: String? = "featurehub-mr-dacha2"

  private val log: Logger = LoggerFactory.getLogger(PubsubDachaEventsListener::class.java)

  init {
    DeclaredConfigResolver.resolve(this)

    pubSubFactory.makeUniqueSubscriber(topicName!!, "dacha2-listener") { msg ->
      try {
        eventListener.process(msg)
      } catch (e: Exception) {
        log.error("dacha2 pubsub: Unable to process incoming event")
      }
      true //
    }

    log.info("dacha2: pubsub listening to {}", topicName)
  }
}
