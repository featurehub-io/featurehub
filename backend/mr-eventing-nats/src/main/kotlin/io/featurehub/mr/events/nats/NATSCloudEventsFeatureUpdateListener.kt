package io.featurehub.mr.events.nats

import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.events.common.listeners.EdgeUpdateListener
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.publish.NATSSource
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NATSCloudEventsFeatureUpdateListener @Inject constructor(
  nats : NATSSource,
  private val updateListener: EdgeUpdateListener) {
  private val featureUpdaterDispatcher: Dispatcher
  private val log: Logger = LoggerFactory.getLogger(NATSCloudEventsFeatureUpdateListener::class.java)
  private val featureUpdaterSubject: String

  init {
    featureUpdaterSubject = "/feature-updates-v2"

    log.info("Listening for feature updates on {}", featureUpdaterSubject)

    // this is a QUEUE, not a pub/sub topic
    featureUpdaterDispatcher = nats.connection.createDispatcher(object: MessageHandler {
      override fun onMessage(msg: Message) {
        streamingFeatureUpdate( msg)
      }

    }).subscribe(featureUpdaterSubject, "feature-updates")
  }

  @PreDestroy
  fun close() {
    featureUpdaterDispatcher.unsubscribe(featureUpdaterSubject)
  }

  private fun streamingFeatureUpdate(msg: Message) {
    NatsMessageFactory.createReader(msg).toEvent()?.let { event ->
      CacheJsonMapper.fromEventData(event, StreamedFeatureUpdate::class.java)?.let {
        updateListener.processUpdate(it)
      }
    }
  }
}
