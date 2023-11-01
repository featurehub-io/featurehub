package io.featurehub.messaging

import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import java.util.concurrent.ExecutorService

interface MessagingConfig {
  val executor: ExecutorService?

  val enabled: Boolean
}

class MessagingConfigImpl @Inject constructor(supplier: ExecutorSupplier) : MessagingConfig {
  override val executor: ExecutorService?
  override val enabled: Boolean
    get() = isEnabled()

  init {
    executor =  if (isEnabled()) supplier.executorService(threadPoolSize()) else null
  }

  companion object {
    fun isEnabled() = FallbackPropertyConfig.getConfig("messaging.publish.enabled")?.lowercase() == "true"
    fun threadPoolSize(): Int = FallbackPropertyConfig.getConfig("messaging.publisher.thread-pool")?.toInt() ?: 4
  }
}
