package io.featurehub.messaging.service

import io.featurehub.messaging.model.FeatureMessagingUpdate

interface FeatureMessagingCloudEventPublisher {
  fun publishFeatureMessagingUpdate(
    hooks: List<MappedSupportedConfig>,
    webhookEnvironmentInfo: Map<String, String>,
    converterHandler: () -> FeatureMessagingUpdate
  )
}
