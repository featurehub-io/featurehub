package io.featurehub.app.db.utils

import cd.connect.context.ConnectContext
import io.ebean.event.changelog.ChangeLogPrepare
import io.ebean.event.changelog.ChangeSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FeatureHubChangeLog : ChangeLogPrepare {
  private val log: Logger = LoggerFactory.getLogger(FeatureHubChangeLog::class.java)

  init {
    log.info("changeset logging is enabled")
  }

  override fun prepare(changeSet: ChangeSet?): Boolean {
    if (changeSet != null) {
      val user = ConnectContext.get("user", java.util.Map::class.java)

      if (user != null) {
        user?.get("id")?.let {
          changeSet.userId = it.toString()
        }
        user?.get("email")?.let {
          changeSet.userContext.put("email", it.toString())
        }
      }
    }

    return true
  }
}
