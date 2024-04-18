package io.featurehub.db.api

import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.model.UpdatedSystemConfig
import io.featurehub.utils.FallbackPropertyConfig
import java.util.*

interface SystemConfigApi {
  class UpdateSystemConfigFailedException(msg: String) : RuntimeException(msg)
  class NoSuchKeyException : RuntimeException()

  @Throws(UpdateSystemConfigFailedException::class)
  fun updateConfigs(configs: List<UpdatedSystemConfig>, whoUpdated: UUID?, allowInternal: Boolean = false): List<SystemConfig>

  fun findConfigs(filters: List<String>): List<SystemConfig>
  fun isEnabled(configSection: String): Boolean

  @Throws(NoSuchKeyException::class)
  fun decryptSystemConfig(key: String, mapKey: String?): String?

  companion object {
    val systemConfigEnabled = FallbackPropertyConfig.getConfig("system-config.enabled","false") == "true"
  }
}
