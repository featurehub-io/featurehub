package io.featurehub.edge.events

import io.cloudevents.CloudEvent
import io.featurehub.dacha.model.PublishFeatureValues
import io.featurehub.edge.StreamingFeatureController
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface EdgeSubscriber {
  fun process(event: CloudEvent): Boolean
}

@LifecyclePriority(LifecyclePriority.INTERNAL_PRIORITY_END)
class EdgeSubscriberListener @Inject constructor(private val controller: StreamingFeatureController, cloudEventReceiverRegistry: CloudEventReceiverRegistry):
  LifecycleListener {
  init {
    cloudEventReceiverRegistry.listen(PublishFeatureValues::class.java) { msg, ce -> controller.updateFeatures(msg) }
  }
}

//class EdgeSubscriberImpl @Inject constructor(private val controller: StreamingFeatureController) : EdgeSubscriber {
//  private val log: Logger = LoggerFactory.getLogger(EdgeSubscriberImpl::class.java)
//
//  override fun process(event: CloudEvent): Boolean {
//    if (event.subject == PublishFeatureValues.CLOUD_EVENT_SUBJECT && event.type == PublishFeatureValues.CLOUD_EVENT_TYPE) {
//      CacheJsonMapper.fromEventData(event, PublishFeatureValues::class.java)?.let {
//        controller.updateFeatures(it)
//      } ?: log.error("failed to decode event {}", event)
//    }
//
//    return true
//  }
//
//}

