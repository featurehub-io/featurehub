package io.featurehub.mr.fhos

import io.featurehub.db.services.SystemConfigChange
import io.featurehub.mr.model.UpdatedSystemConfig
import io.featurehub.systemcfg.KnownSystemConfigSource
import io.featurehub.systemcfg.ReadOnlySystemConfig
import io.featurehub.systemcfg.ValidSystemConfig
import java.util.UUID

/**
 * This is only configured for FHOS as it doesn't make sense for use in SaaS.
 */
class DachaPublisherConfigSource : KnownSystemConfigSource {
  override fun presaveUpdateCheck(
    changes: List<UpdatedSystemConfig>,
    orgId: UUID
  ): String? {
    return null
  }

  override fun configUpdateCheck(
    changes: Map<String, SystemConfigChange>,
    orgId: UUID
  ) {
  }

  override val name: String
    get() = "dacha-system-wide-publish"
  override val knownConfig: List<ValidSystemConfig>
    get() = listOf(ValidSystemConfig("refresh.cache", "System wide cache respublish", false, KnownSystemConfigSource.boolRef, false, null, false))
  override val readOnlyConfg: List<ReadOnlySystemConfig>
    get() = listOf(ReadOnlySystemConfig("refresh.cache", "System wide cache republish", null))
}
