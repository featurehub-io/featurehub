package io.featurehub.db.utils

import io.featurehub.health.HealthSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseHealthSource
  @Inject
  constructor(val ebeanSource: EbeanSource) : HealthSource {

  override val healthy: Boolean
    get() {
      try {
        ebeanSource.ebeanServer.sqlQuery("/* EbeanHealthCheck */ SELECT 1").findOne()
        return true
      } catch (e: Throwable) {
        return false
      }
    }
  override val sourceName: String
    get() = "Database connectivity"
}
