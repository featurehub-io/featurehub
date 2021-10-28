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
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

open class CommonDbFeature : Feature {
  private val log: Logger = LoggerFactory.getLogger(CommonDbFeature::class.java)

  @ConfigKey("db.username")
  var username: String? = null

  @ConfigKey("db.password")
  var password: String? = null

  @ConfigKey("db.connections")
  var maxConnections: Int? = -1

  @ConfigKey("db.run-migrations")
  var runMigrations: Boolean? = true

  init {
      DeclaredConfigResolver.resolve(this)
  }

  internal class FeatureHubDatabaseSource constructor(val dsConfig: DataSourceConfig, val migrationConfig: MigrationConfig);

  private fun supportHome(dbUrl: String): String =
    dbUrl.replace("\$home", System.getProperty("user.home"))


  private fun configureDataSource(prefix: String): FeatureHubDatabaseSource {

    val p = Properties()
    val propertyPrefix = prefix + ".";
    System.getProperties().forEach { k: Any?, v: Any? ->
      if (k is String && k.startsWith(propertyPrefix)) {
        p["datasource.$k"] = v
      }
    }

    val sysEnvPrefix = prefix.uppercase() + "_"
    System.getenv().forEach { k, v ->
      if (k.startsWith(sysEnvPrefix)) {
        p["datasource." + k.lowercase().replace("_", ".")] = v
      } else if (k.startsWith(propertyPrefix)) {
        p["datasource.$k"] = v
      }
    }

    val dsConfig = DataSourceConfig()

    dsConfig.loadSettings(p, prefix)

    dsConfig.url = supportHome(dsConfig.url)

    log.info("connecting to database {}", dsConfig.url)

    if (dsConfig.username == null) {
      dsConfig.username = username
    }

    if (dsConfig.password == null) {
      dsConfig.password = password
    }

    if (maxConnections != -1) {
      dsConfig.maxConnections = maxConnections!!
    }

    val defaultDriver: String
    val migrationConfig = MigrationConfig()

    migrationConfig.load(p)

    val databaseUrl = dsConfig.url!!

    if (databaseUrl.contains("postgres")) {
      migrationConfig.migrationPath = "classpath:/dbmigration/postgres"
      migrationConfig.platformName = DbPlatformNames.POSTGRES
      defaultDriver = "org.postgresql.Driver"
    } else if (databaseUrl.contains("mysql") || databaseUrl.contains("mariadb")) {
      migrationConfig.migrationPath = "classpath:/dbmigration/mysql"
      migrationConfig.platformName = DbPlatformNames.MYSQL
      defaultDriver = "com.mysql.jdbc.Driver"
    } else if (databaseUrl.contains("sqlserver")) {
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

    return FeatureHubDatabaseSource(dsConfig, migrationConfig)
  }

  override fun configure(context: FeatureContext): Boolean {
    val dsMasterConfig = configureDataSource("db")

    dsMasterConfig.dsConfig.maxConnections = 3

    val dsReplicaConfig = if (FallbackPropertyConfig.getConfig("db-replica.url") == null)
      null
    else
      configureDataSource("db-replica")

    val dbConfig = DatabaseConfig()

    dbConfig.databasePlatformName = dsMasterConfig.migrationConfig.platformName

    val pool = DataSourceFactory.create("generic", dsMasterConfig.dsConfig)

    if (runMigrations!!) {
      val runner = MigrationRunner(dsMasterConfig.migrationConfig)

      runner.run(pool)
    }

    dbConfig.isDefaultServer = true
    dbConfig.dataSource = pool
    dbConfig.add(UUIDIdGenerator())
    dbConfig.isRunMigration = false

    log.info("Database Master configured at {}", dsMasterConfig.dsConfig.url)

    if (dsReplicaConfig != null) {
      log.info("Database Read Replica configured at {}", dsReplicaConfig.dsConfig.url)
      dbConfig.readOnlyDataSourceConfig = dsReplicaConfig.dsConfig
    }

    enhanceDbConfig(dbConfig)

    val database = DatabaseFactory.create(dbConfig)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(database).to(Database::class.java).`in`(Singleton::class.java)
        bind(DatabaseHealthSource::class.java).to(HealthSource::class.java)
      }
    })

    context.register(object: ContainerLifecycleListener {
      override fun onStartup(container: Container?) {
      }

      override fun onReload(container: Container?) {
      }

      override fun onShutdown(container: Container?) {
        try {
          database.shutdown()
        } catch (ignored: Exception) {
        }
      }
    });

    return true
  }

  open fun enhanceDbConfig(config: DatabaseConfig) {

  }
}
