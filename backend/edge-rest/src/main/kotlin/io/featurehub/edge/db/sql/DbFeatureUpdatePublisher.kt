package io.featurehub.edge.db.sql

import io.featurehub.edge.rest.FeatureUpdatePublisher
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import jakarta.inject.Inject

class DbFeatureUpdatePublisher @Inject constructor(private val featureUpdateListener: FeatureUpdateListener) : FeatureUpdatePublisher {
  override fun publishFeatureChangeRequest(
    featureUpdate: StreamedFeatureUpdate,
    namedCache: String
  ) {
    featureUpdateListener.processUpdate(featureUpdate)
  }
}
