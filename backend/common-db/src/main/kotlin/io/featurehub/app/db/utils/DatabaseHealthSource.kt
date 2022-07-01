package io.featurehub.app.db.utils

import io.ebean.Database
import io.featurehub.health.HealthSource
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class DatabaseHealthSource
@Inject
constructor(val database: Database) : HealthSource {
  private val log: Logger = LoggerFactory.getLogger(DatabaseHealthSource::class.java)

  override val healthy: Boolean
    get() {
      MDC.putCloseable("connect.disable-logs", "true").use {
        try {
          database.sqlQuery("/* EbeanHealthCheck */ SELECT 1").findOne()
          return true
        } catch (e: Throwable) {
          return false
        }
      }
    }
  override val sourceName: String
    get() = "Database connectivity"
}
