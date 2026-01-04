package io.featurehub.mr.events.service

import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.events.CloudEventReceiverRegistry
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.mr.events.common.listeners.FoundationFeatureUpdateListenerImpl
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import jakarta.inject.Inject
import org.slf4j.LoggerFactory

open class FeatureUpdateListenerImpl @Inject constructor(
  featureUpdateBySDKApi: FeatureUpdateBySDKApi,
  cloudEventReceiverRegistry: CloudEventReceiverRegistry
) : FoundationFeatureUpdateListenerImpl(featureUpdateBySDKApi), FeatureUpdateListener {
  init {
    cloudEventReceiverRegistry.listen(StreamedFeatureUpdate::class.java) { it, ce ->
      processUpdate(it)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(FeatureUpdateListener::class.java)
  }
}
