package io.featurehub.messaging.service

import io.featurehub.messaging.model.FeatureMessagingUpdate

interface FeatureMessagingCloudEventPublisher {
  fun publishFeatureMessagingUpdate(featureMessagingUpdate: FeatureMessagingUpdate)
}
