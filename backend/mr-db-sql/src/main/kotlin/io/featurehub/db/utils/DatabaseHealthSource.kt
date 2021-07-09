package io.featurehub.db.utils

import io.ebean.Database
import io.featurehub.health.HealthSource
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class DatabaseHealthSource
  @Inject
  constructor(val database: Database) : HealthSource {

  override val healthy: Boolean
    get() {
      try {
        database.sqlQuery("/* EbeanHealthCheck */ SELECT 1").findOne()
        return true
      } catch (e: Throwable) {
        return false
      }
    }
  override val sourceName: String
    get() = "Database connectivity"
}
