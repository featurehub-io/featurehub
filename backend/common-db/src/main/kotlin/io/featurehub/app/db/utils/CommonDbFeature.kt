package io.featurehub.app.db.utils

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.ebean.Database
import io.ebean.DatabaseFactory
import io.ebean.config.DatabaseConfig
import io.ebean.datasource.DataSourceConfig
import io.ebean.datasource.DataSourceFactory
import io.ebean.migration.DbPlatformNames
import io.ebean.migration.MigrationConfig
import io.ebean.migration.MigrationRunner
import io.featurehub.health.HealthSource
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class CommonDbFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(CommonDbFeature::class.java)

  @ConfigKey("db.url")
  var databaseUrl: String? = null

  @ConfigKey("db.username")
  var username: String? = null

  @ConfigKey("db.password")
  var password: String? = null

  @ConfigKey("db.connections")
  var maxConnections: Int? = null

  @ConfigKey("db.run-migrations")
  var runMigrations: Boolean? = true

  init {
      DeclaredConfigResolver.resolve(this)
  }

  override fun configure(context: FeatureContext): Boolean {

    val dbConfig = DatabaseConfig()

    val p = Properties()
    System.getProperties().forEach { k: Any?, v: Any? ->
      if (k is String && k.startsWith("db.")) {
        p[k] = v
      }
    }

    System.getenv().forEach { k, v ->
      if (k.startsWith("DB_")) {
        p[k.lowercase().replace("_", ".")] = v
      }
    }

    val dsConfig = DataSourceConfig()

    dsConfig.url = databaseUrl!!.replace("\$home", System.getProperty("user.home"))
    log.info("connecting to database {}", dsConfig.url)

    dsConfig.username = username
    dsConfig.password = password
    dsConfig.maxConnections = maxConnections!!
    dsConfig.loadSettings(p, "db")

    val defaultDriver: String
    val migrationConfig = MigrationConfig()

    migrationConfig.load(p)

    if (databaseUrl?.contains("postgres") == true) {
      migrationConfig.migrationPath = "classpath:/dbmigration/postgres"
      migrationConfig.platformName = DbPlatformNames.POSTGRES
      defaultDriver = "org.postgresql.Driver"
    } else if (databaseUrl?.contains("mysql") == true || databaseUrl?.contains("mariadb") == true) {
      migrationConfig.migrationPath = "classpath:/dbmigration/mysql"
      migrationConfig.platformName = DbPlatformNames.MYSQL
      defaultDriver = "com.mysql.jdbc.Driver"
    } else if (databaseUrl?.contains("sqlserver") == true) {
      migrationConfig.migrationPath = "classpath:/dbmigration/mssql"
      migrationConfig.platformName = DbPlatformNames.SQLSERVER
      defaultDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    } else {
      migrationConfig.migrationPath = "classpath:/dbmigration/h2"
      migrationConfig.platformName = DbPlatformNames.H2
      defaultDriver = "org.h2.Driver"
    }

    if (dsConfig.driver == null) {
      dsConfig.driver = defaultDriver
    }

    dbConfig.databasePlatformName = migrationConfig.platformName
    val pool = DataSourceFactory.create("generic", dsConfig)

    if (runMigrations!!) {
      val runner = MigrationRunner(migrationConfig)

      runner.run(pool)
    }


    dbConfig.isDefaultServer = true
    dbConfig.dataSource = pool
    dbConfig.add(UUIDIdGenerator())
    dbConfig.isRunMigration = false

    val database = DatabaseFactory.create(dbConfig)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(database).to(Database::class.java).`in`(Singleton::class.java)
        bind(DatabaseHealthSource::class.java).to(HealthSource::class.java)
      }
    })

    return true
  }
}
