package io.featurehub.edge.events

import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.edge.rest.FeatureUpdatePublisher
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

interface CloudEventsEdgePublisher {
  fun encodeAsJson(): Boolean
  fun publish(event: CloudEvent)
}

class CloudEventsFeatureUpdatePublisherImpl @Inject constructor(private val cloudEventsPublisher: CloudEventsEdgePublisher) : FeatureUpdatePublisher {
  private val log: Logger = LoggerFactory.getLogger(CloudEventsFeatureUpdatePublisherImpl::class.java)

  override fun publishFeatureChangeRequest(featureUpdate: StreamedFeatureUpdate, namedCache: String) {
    val event = CloudEventBuilder.v1().newBuilder()
    event.withSubject(StreamedFeatureUpdate.CLOUD_EVENT_SUBJECT)
    event.withId(UUID.randomUUID().toString())
    event.withType(StreamedFeatureUpdate.CLOUD_EVENT_TYPE)
    event.withSource(URI("http://edge"))
    event.withContextAttribute("cachename", namedCache)
    event.withTime(OffsetDateTime.now())

    CacheJsonMapper.toEventData(event, featureUpdate, !cloudEventsPublisher.encodeAsJson())

    try {
      cloudEventsPublisher.publish(event.build())
    } catch (e: Exception) {
      log.error("failed to publish event update", e)
    }
  }

}
