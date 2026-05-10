package io.featurehub.systemcfg

import io.featurehub.db.services.SystemConfigChange
import io.featurehub.mr.model.SystemConfig
import io.featurehub.mr.model.MaintenanceInfo
import io.featurehub.mr.model.UpdatedSystemConfig
import java.time.OffsetDateTime
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
    const val cfg_startTime = "maintenance.startTime"
    const val cfg_endTime = "maintenance.endTime"

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
      ),
      ValidSystemConfig(
        cfg_startTime,
        "UTC datetime when the maintenance window begins and users are locked out",
        false,
        KnownSystemConfigSource.stringRef,
        false, null
      ),
      ValidSystemConfig(
        cfg_endTime,
        "UTC datetime when the maintenance window ends and login is allowed again",
        false,
        KnownSystemConfigSource.stringRef,
        false, null
      )
    )

    @JvmStatic
    /**
     * Computes [MaintenanceInfo] based on current time relative to startTime/endTime.
     *
     * Rules:
     * - active=false in config → null (204, no banner)
     * - past endTime → null (204, maintenance over)
     * - before startTime → MaintenanceInfo(active=false, ...) — pre-maintenance warning banner
     * - startTime..endTime (or no startTime, before endTime, or no times at all) → MaintenanceInfo(active=true, ...) — lock out users
     */
    fun computeMaintenanceInfo(configs: List<SystemConfig>): MaintenanceInfo? {
      val active = configs.find { it.key == cfg_active }?.value as? Boolean ?: false
      if (!active) return null

      val now = OffsetDateTime.now()
      val message = configs.find { it.key == cfg_message }?.value as? String

      val startTimeStr = configs.find { it.key == cfg_startTime }?.value as? String
      val endTimeStr = configs.find { it.key == cfg_endTime }?.value as? String

      val startTime = startTimeStr?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
      val endTime = endTimeStr?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }

      // Maintenance window has passed entirely
      if (endTime != null && now.isAfter(endTime)) return null

      // We're before the start time — show pre-maintenance warning but don't lock out
      val isActive = startTime == null || !now.isBefore(startTime)

      return MaintenanceInfo()
        .active(isActive)
        .message(message)
        .startTime(startTime)
        .endTime(endTime)
    }
  }

  override val name: String
    get() = "maintenance"
  override val knownConfig: List<ValidSystemConfig>
    get() = config
  override val readOnlyConfg: List<ReadOnlySystemConfig>
    get() = listOf()
}
