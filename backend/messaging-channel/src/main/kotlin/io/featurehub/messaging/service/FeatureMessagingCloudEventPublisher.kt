package io.featurehub.messaging.service

import io.featurehub.messaging.model.FeatureMessagingUpdate

interface FeatureMessagingCloudEventPublisher {
  /**
   * This is set up by the initializer
   */
  fun setHooks(hooks: List<MappedSupportedConfig>)

  fun publishFeatureMessagingUpdate(
    webhookEnvironmentInfo: Map<String, String>,
    converterHandler: () -> FeatureMessagingUpdate
  )
}
