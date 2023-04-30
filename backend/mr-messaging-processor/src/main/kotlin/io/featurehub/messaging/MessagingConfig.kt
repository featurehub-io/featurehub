package io.featurehub.messaging

import io.featurehub.utils.FallbackPropertyConfig

class MessagingConfig {

  companion object {
    fun isEnabled() = FallbackPropertyConfig.getConfig("messaging.publish.enabled")?.lowercase() != "false"
    fun threadPoolSize(): Int = FallbackPropertyConfig.getConfig("messaging.publisher.thread-pool")?.toInt() ?: 4
  }
}
