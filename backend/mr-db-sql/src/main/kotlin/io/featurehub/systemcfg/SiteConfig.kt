package io.featurehub.systemcfg

import io.featurehub.db.services.SystemConfigChange
import io.featurehub.mr.model.UpdatedSystemConfig
import java.util.*

class SiteConfig : KnownSystemConfigSource {
  override fun presaveUpdateCheck(changes: List<UpdatedSystemConfig>, orgId: UUID): String? {
    return null
  }

  override fun configUpdateCheck(changes: Map<String, SystemConfigChange>, orgId: UUID) {
  }

  companion object {
    const val cfg_url = "site.url"
    const val cfg_enableRobots = "site.enabledRobots"
    const val cfg_redirectInvaildHosts = "site.redirectInvalidHosts"

    val config = listOf(
      ValidSystemConfig(
        cfg_url,
        "The site's url",
        false,
        KnownSystemConfigSource.stringRef,
        true, "http://localhost:8085"
      ),
      ValidSystemConfig(
        cfg_enableRobots,
        "Allow robots to index that this site exists",
        false,
        KnownSystemConfigSource.boolRef,
        false, false
      ),
      ValidSystemConfig(
        cfg_redirectInvaildHosts,
        "Redirect invalid Host headers, off by default so url can be configured",
        false,
        KnownSystemConfigSource.boolRef,
        false, false
      )
    )
  }

  override val name: String
    get() = "site"
  override val knownConfig: List<ValidSystemConfig>
    get() = config
  override val readOnlyConfg: List<ReadOnlySystemConfig>
    get() = listOf()
}
