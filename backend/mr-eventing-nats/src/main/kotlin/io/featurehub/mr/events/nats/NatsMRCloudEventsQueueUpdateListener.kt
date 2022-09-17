package io.featurehub.mr.events.nats

import io.featurehub.events.nats.NatsListener
import io.featurehub.mr.events.common.listeners.CloudEventListener
import io.featurehub.publish.NATSSource
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsMRCloudEventsQueueUpdateListener @Inject constructor(
  nats : NATSSource,
  private val updateListener: CloudEventListener) {
  private val featureUpdaterDispatcher: NatsListener
  private val log: Logger = LoggerFactory.getLogger(NatsMRCloudEventsQueueUpdateListener::class.java)
  private val updatesSubject: String = "/mr-updates-queue"

  init {
    log.info("Listening for MR updates on {}", updatesSubject)

    featureUpdaterDispatcher = nats.createQueueListener(updatesSubject, "mr-update") { event ->
      updateListener.process(event)
    }
  }

  @PreDestroy
  fun close() {
    featureUpdaterDispatcher.close()
  }
}
