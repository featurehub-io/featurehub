package io.featurehub.edge.events

import io.cloudevents.CloudEvent
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.jersey.config.CacheJsonMapper
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface EdgeSubscriber {
  fun process(event: CloudEvent): Boolean
}

class EdgeSubscriberImpl @Inject constructor(private val controller: StreamingFeatureController) : EdgeSubscriber {
  private val log: Logger = LoggerFactory.getLogger(EdgeSubscriberImpl::class.java)

  override fun process(event: CloudEvent): Boolean {
    if (event.subject == PublishFeatureValues.CLOUD_EVENT_SUBJECT && event.type == PublishFeatureValues.CLOUD_EVENT_TYPE) {
      CacheJsonMapper.fromEventData(event, PublishFeatureValues::class.java)?.let {
        controller.updateFeatures(it)
      } ?: log.error("failed to decode event {}", event)
    }

    return true
  }

}

