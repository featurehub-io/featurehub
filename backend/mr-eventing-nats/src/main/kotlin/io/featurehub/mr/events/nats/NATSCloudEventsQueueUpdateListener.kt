package io.featurehub.mr.events.nats

import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.events.common.listeners.CloudEventListener
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.publish.NATSSource
import io.nats.client.Dispatcher
import io.nats.client.Message
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NATSCloudEventsQueueUpdateListener @Inject constructor(
  nats : NATSSource,
  private val updateListener: CloudEventListener) {
  private val featureUpdaterDispatcher: Dispatcher
  private val log: Logger = LoggerFactory.getLogger(NATSCloudEventsQueueUpdateListener::class.java)
  private val updatesSubject: String = "/mr-updates-queue"

  init {

    log.info("Listening for MR updates on {}", updatesSubject)

    // this is a QUEUE, not a pub/sub topic
    featureUpdaterDispatcher = nats.connection.createDispatcher { msg -> eventReader(msg) }
      .subscribe(updatesSubject, "mr-updates")
  }

  @PreDestroy
  fun close() {
    featureUpdaterDispatcher.unsubscribe(updatesSubject)
  }

  private fun eventReader(msg: Message) {
    NatsMessageFactory.createReader(msg).toEvent()?.let { event ->
      updateListener.process(event)
    } ?: log.error("mr-update-queue: Unable to decode cloud event {}", msg)
  }
}
