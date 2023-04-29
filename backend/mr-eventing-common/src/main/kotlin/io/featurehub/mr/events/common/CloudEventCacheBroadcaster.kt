package io.featurehub.mr.events.common

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.events.CloudEventPublisher
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime

class CloudEventCacheBroadcaster @Inject constructor(
  private val cloudEventsPublisher: CloudEventPublisher
) : CacheBroadcast {
  private val log: Logger = LoggerFactory.getLogger(CloudEventCacheBroadcaster::class.java)

  fun publish(
    subject: String,
    data: Any,
    id: String?,
    type: String,
  ) {
    try {
      val event = CloudEventBuilder.v1().newBuilder()
      event.withSubject(subject)
      event.withId(id ?: "000")
      event.withType(type)
      event.withSource(URI("http://management-service"))
      event.withTime(OffsetDateTime.now())

      cloudEventsPublisher.publish(type, data, event)
    } catch (e: Exception) {
      log.error("failed", e)
    }
  }

  override fun publishEnvironment(eci: PublishEnvironment) {
    if (cloudEventsPublisher.hasListeners(PublishEnvironment.CLOUD_EVENT_TYPE)) {
      log.trace("cloudevent: publish environment {}", eci)
      publish(PublishEnvironment.CLOUD_EVENT_SUBJECT, eci, eci.environment.id.toString(),
        PublishEnvironment.CLOUD_EVENT_TYPE)
    }
  }

  override fun publishServiceAccount(saci: PublishServiceAccount) {
    if (cloudEventsPublisher.hasListeners(PublishServiceAccount.CLOUD_EVENT_TYPE)) {
      log.trace("cloudevent: publish service account {}", saci)
      publish(
        PublishServiceAccount.CLOUD_EVENT_SUBJECT,
        saci,
        saci.serviceAccount?.id.toString(),
        PublishServiceAccount.CLOUD_EVENT_TYPE
      )
    }
  }

  override fun publishFeatures(features: PublishFeatureValues) {
    if (features.features.isNotEmpty() && cloudEventsPublisher.hasListeners(PublishFeatureValues.CLOUD_EVENT_TYPE)) {
      // all updates are the same type for the same environment, not that it really matters
      val firstFeature = features.features[0]

      log.trace("cloudevent: publish features {}", features)

      publish(
        PublishFeatureValues.CLOUD_EVENT_SUBJECT, features,
        "${firstFeature.environmentId}/${firstFeature.feature.feature.key}", PublishFeatureValues.CLOUD_EVENT_TYPE
      )
    }
  }
}
