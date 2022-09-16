package io.featurehub.mr.events.nats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.cloudevents.nats.NatsMessageFactory
import io.featurehub.events.KnownEventSubjects
import io.featurehub.mr.events.common.CloudEventBroadcasterWriter
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NATSCloudEventsBroadcaster @Inject constructor(private val nats: NATSSource) : CloudEventBroadcasterWriter {
  @ConfigKey("dacha2.environment.nats-channel-name")
  private var environmentSubject: String? = "featurehub/environment"
  @ConfigKey("dacha2.service-account.nats-channel-name")
  private var serviceAccountSubject: String? = "featurehub/service-account"
  @ConfigKey("dacha2.features.nats-channel-name")
  private var featureSubject: String? = "featurehub/feature"

  private val log: Logger = LoggerFactory.getLogger(NATSCloudEventsBroadcaster::class.java)

  init {
      DeclaredConfigResolver.resolve(this)
  }

  override fun encodePureJson(): Boolean {
    return false
  }

  override fun publish(event: CloudEvent) {
    val subject = when (event.subject) {
      KnownEventSubjects.Management.featureUpdates -> featureSubject
      KnownEventSubjects.Management.serviceAccountUpdate -> serviceAccountSubject
      KnownEventSubjects.Management.environmentUpdate -> environmentSubject
      else -> null
    }

    if (subject != null) {
      nats.connection.publish(NatsMessageFactory.createWriter().writeBinary(event))
    } else {
      log.error("unknown message type {}", event)
      throw RuntimeException("Attempting to send unknown message type")
    }
  }

}
