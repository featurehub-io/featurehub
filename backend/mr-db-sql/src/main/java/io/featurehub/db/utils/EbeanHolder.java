package io.featurehub.db.utils;

import io.ebean.Database;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import io.ebean.migration.DbPlatformNames;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.Properties;

public class EbeanHolder implements EbeanSource {
  private static final Logger log = LoggerFactory.getLogger(EbeanHolder.class);
  private final Database ebeanServer;
  private final DatabaseConfig config;

  public EbeanHolder(String dbUrl, String dbUsername, String dbPassword, int maxConnections, String dbDriver) {
    this.config = new DatabaseConfig();

    Properties p = new Properties();
    System.getProperties().forEach((k, v) -> {
      if (k instanceof String && ((String)k).startsWith("db.")) {
        p.put(k, v);
      }
    });

    // support environments like ECS that can only support environment variables
    System.getenv().forEach((k, v) -> {
      if (k.startsWith("DB.")) {
        p.put(k.toLowerCase(), v);
      }
    });

    dbUrl = dbUrl.replace("$home", System.getProperty("user.home"));

    DataSourceConfig dsConfig = new DataSourceConfig();

    log.info("database url: {}", dbUrl);

    dsConfig.setUrl(dbUrl);
    dsConfig.setUsername(dbUsername);
    dsConfig.setPassword(dbPassword);
    dsConfig.setMaxConnections(maxConnections);
    dsConfig.loadSettings(p, "db");

    String defaultDriver;
    MigrationConfig migrationConfig = new MigrationConfig();

    migrationConfig.load(p);

    if (dbUrl.contains("mysql") || dbUrl.contains("mariadb")) {
      migrationConfig.setMigrationPath("classpath:/dbmigration/mysql");
      migrationConfig.setPlatformName(DbPlatformNames.MYSQL);
      defaultDriver = "com.mysql.jdbc.Driver";
    } else if (dbUrl.contains("sqlserver")) {
      migrationConfig.setMigrationPath("classpath:/dbmigration/mssql");
      migrationConfig.setPlatformName(DbPlatformNames.SQLSERVER);
      defaultDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    } else if (dbUrl.contains("postgres")) {
      migrationConfig.setMigrationPath("classpath:/dbmigration/postgres");
      migrationConfig.setPlatformName(DbPlatformNames.POSTGRES);
      defaultDriver = "org.postgresql.Driver";
    } else {
      migrationConfig.setMigrationPath("classpath:/dbmigration/h2");
      migrationConfig.setPlatformName(DbPlatformNames.H2);
      defaultDriver = "org.h2.Driver";
    }

    if (dbDriver == null) {
      dsConfig.setDriver(defaultDriver);
    } else {
      dsConfig.setDriver(dbDriver);
    }

    config.setDatabasePlatformName(migrationConfig.getPlatformName());
    final DataSourcePool pool = DataSourceFactory.create("generic", dsConfig);
    MigrationRunner runner = new MigrationRunner(migrationConfig);

    runner.run(pool);
    config.setDataSource(pool);
    config.add(new UUIDIdGenerator());
    config.setRunMigration(false);


    this.ebeanServer = io.ebean.DatabaseFactory.create(config);
  }

  @Override
  public Database getEbeanServer() {
    return ebeanServer;
  }

  @Override
  public DataSource getDatasource() {
    return config.getDataSource();
  }
}
