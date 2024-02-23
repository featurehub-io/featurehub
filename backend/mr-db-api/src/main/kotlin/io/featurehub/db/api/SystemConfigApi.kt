package io.featurehub.db.api

import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.model.UpdatedSystemConfig
import java.util.*

interface SystemConfigApi {
  fun updateConfigs(configs: List<UpdatedSystemConfig>, whoUpdated: UUID): List<SystemConfig>

  fun findConfigs(filters: List<String>): List<SystemConfig>
}
