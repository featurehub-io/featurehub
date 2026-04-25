package io.featurehub.systemcfg

import io.featurehub.db.services.SystemConfigChange
import io.featurehub.mr.model.UpdatedSystemConfig
import java.util.UUID

class MaintenanceConfig : KnownSystemConfigSource {
  override fun presaveUpdateCheck(changes: List<UpdatedSystemConfig>, orgId: UUID): String? {
    return null
  }

  override fun configUpdateCheck(changes: Map<String, SystemConfigChange>, orgId: UUID) {
  }

  companion object {
    const val cfg_active = "maintenance.active"
    const val cfg_message = "maintenance.message"

    val config = listOf(
      ValidSystemConfig(
        cfg_active,
        "Whether the system is currently in a maintenance window",
        false,
        KnownSystemConfigSource.boolRef,
        true, false
      ),
      ValidSystemConfig(
        cfg_message,
        "The message to display in the maintenance banner",
        false,
        KnownSystemConfigSource.stringRef,
        false, null
      )
    )
  }

  override val name: String
    get() = "maintenance"
  override val knownConfig: List<ValidSystemConfig>
    get() = config
  override val readOnlyConfg: List<ReadOnlySystemConfig>
    get() = listOf()
}
