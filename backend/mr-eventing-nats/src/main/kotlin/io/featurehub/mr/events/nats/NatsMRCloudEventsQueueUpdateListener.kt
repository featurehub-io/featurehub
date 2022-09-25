package io.featurehub.mr.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.events.nats.NatsListener
import io.featurehub.publish.NATSSource
import jakarta.annotation.PreDestroy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NatsMRCloudEventsQueueUpdateListener @Inject constructor(
  nats : NATSSource, registry: CloudEventReceiverRegistry) {
  private val featureUpdaterDispatcher: NatsListener
  private val log: Logger = LoggerFactory.getLogger(NatsMRCloudEventsQueueUpdateListener::class.java)
  @ConfigKey("cloudevents.edge-mr.nats.channel-name")
  private val updatesSubject: String = "featurehub/mr-updates-queue"
  @ConfigKey("cloudevents.edge-mr.nats.queue-name")
  private val updatesQueue: String = "featurehub/mr-update"

  init {
    DeclaredConfigResolver.resolve(this)

    log.info("Listening for MR updates on {}", updatesSubject)

    featureUpdaterDispatcher = nats.createQueueListener(updatesSubject, updatesQueue) { event ->
      registry.process(event)
    }
  }

  @PreDestroy
  fun close() {
    featureUpdaterDispatcher.close()
  }
}
