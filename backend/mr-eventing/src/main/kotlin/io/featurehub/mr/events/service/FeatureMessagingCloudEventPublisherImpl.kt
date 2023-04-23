package io.featurehub.mr.events.service

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.mr.events.common.FeatureMessagingCloudEventPublisher
import io.featurehub.mr.events.common.converter.FeatureMessagingConverter
import io.featurehub.mr.events.common.converter.FeatureMessagingParameter
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

class FeatureMessagingCloudEventPublisherImpl(
  private val featureMessagingConverter: FeatureMessagingConverter,
  private val cloudEventPublisher: CloudEventPublisher
) : FeatureMessagingCloudEventPublisher{
  private val log = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)
  override fun publishFeatureMessagingUpdate(featureMessagingParameter: FeatureMessagingParameter) {
    try {
      val featureMessagingUpdate = featureMessagingConverter.toFeatureMessagingUpdate(featureMessagingParameter)
      val event = CloudEventBuilder.v1().newBuilder()
      event.withSubject(FeatureMessagingUpdate.CLOUD_EVENT_SUBJECT)
      event.withId(UUID.randomUUID().toString())
      event.withType(FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
      event.withSource(URI("http://mr"))
      event.withTime(OffsetDateTime.now())
      cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, event)
    } catch (e: Exception) {
      log.error("Failed to publish messaging update for feature {}", featureMessagingParameter.featureValue, e)
    }
  }
}
