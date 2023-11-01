package io.featurehub.messaging.service

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.MessagingConfig
import io.featurehub.messaging.model.FeatureMessagingUpdate
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

class FeatureMessagingCloudEventPublisherImpl @Inject constructor(
  private val cloudEventPublisher: CloudEventPublisher,
  private val messagingConfig: MessagingConfig
) : FeatureMessagingCloudEventPublisher {
  private val log = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)

  override fun publishFeatureMessagingUpdate(featureMessagingUpdate: FeatureMessagingUpdate) {
    messagingConfig.executor?.let {
      it.submit {
        log.trace("publishing feature messaging update for {}", featureMessagingUpdate)
        try {
          val event = CloudEventBuilder.v1().newBuilder()
          event.withSubject(FeatureMessagingUpdate.CLOUD_EVENT_SUBJECT)
          event.withId(UUID.randomUUID().toString())
          event.withType(FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
          event.withSource(URI("http://mr"))
          event.withTime(OffsetDateTime.now())
          cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, event)
        } catch (e: Exception) {
          log.error("Failed to publish messaging update for feature {}", featureMessagingUpdate, e)
        }
      }
    }
  }
}
