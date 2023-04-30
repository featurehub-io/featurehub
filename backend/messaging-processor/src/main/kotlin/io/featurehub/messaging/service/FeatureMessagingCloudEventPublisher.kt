package io.featurehub.messaging.service

import io.featurehub.messaging.converter.FeatureMessagingParameter

interface FeatureMessagingCloudEventPublisher {
  fun publishFeatureMessagingUpdate(featureMessagingParameter: FeatureMessagingParameter)
}
