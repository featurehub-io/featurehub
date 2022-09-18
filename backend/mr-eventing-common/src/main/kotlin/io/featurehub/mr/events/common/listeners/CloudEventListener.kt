package io.featurehub.mr.events.common.listeners

import io.cloudevents.CloudEvent
import io.featurehub.events.KnownEventSubjects
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface CloudEventListener {
  fun process(event: CloudEvent)
}

class CloudEventListenerImpl @Inject constructor(private val edgeUpdateListener: EdgeUpdateListener) : CloudEventListener {
  private val log: Logger = LoggerFactory.getLogger(CloudEventListenerImpl::class.java)

  override fun process(event: CloudEvent) {
    when (event.subject) {
      KnownEventSubjects.ServiceAccountAction.featureUpdates -> processEdgeUpdate(event)
    }
  }

  private fun processEdgeUpdate(event: CloudEvent) {
    CacheJsonMapper.fromEventData(event, StreamedFeatureUpdate::class.java)?.let {
      edgeUpdateListener.processUpdate(it)
    } ?: log.error("Dropped feature update {}", event.toString())
  }
}
