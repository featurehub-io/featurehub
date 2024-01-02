package io.featurehub.messaging.service

import io.featurehub.events.DynamicCloudEventDestination


interface FeatureMessagingCloudEventPublisher {
  /**
   * This is set up by the initializer
   */
  fun setHooks(hooks: List<DynamicCloudEventDestination>)

  val isEnabled: Boolean
}
