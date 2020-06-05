package io.featurehub.db.utils;

import io.ebean.Database;
import io.ebean.annotation.Platform;
import io.ebean.config.DbMigrationConfig;
import io.ebean.config.ServerConfig;
import io.ebean.datasource.DataSourceConfig;

import javax.sql.DataSource;

public class EbeanHolder implements EbeanSource {
  private final Database ebeanServer;
  private final ServerConfig config;

  public EbeanHolder(String dbUrl, String dbUsername, String dbPassword, int maxConnections, String dbDriver) {
    this.config = new ServerConfig();

    DataSourceConfig dsConfig = new DataSourceConfig();

    dsConfig.setUrl(dbUrl);
    dsConfig.setUsername(dbUsername);
    dsConfig.setPassword(dbPassword);
    dsConfig.setMaxConnections(maxConnections);

    String defaultDriver;
    DbMigrationConfig migrationConfig = new DbMigrationConfig();
    if (dbUrl.contains("mysql")) {
      migrationConfig.setMigrationPath("/dbmigration/mysql");
      migrationConfig.setPlatform(Platform.MYSQL);
      defaultDriver = "com.mysql.jdbc.Driver";
    } else if (dbUrl.contains("sqlserver")) {
      migrationConfig.setMigrationPath("/dbmigration/mssql");
      migrationConfig.setPlatform(Platform.SQLSERVER17);
      defaultDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    } else if (dbUrl.contains("postgres")) {
      migrationConfig.setMigrationPath("/dbmigration/postgres");
      migrationConfig.setPlatform(Platform.POSTGRES);
      defaultDriver = "org.postgresql.Driver";
    } else {
      migrationConfig.setMigrationPath("/dbmigration/h2");
      migrationConfig.setPlatform(Platform.H2);
      defaultDriver = "org.h2.Driver";
    }

    if (dbDriver == null) {
      dsConfig.setDriver(defaultDriver);
    } else {
      dsConfig.setDriver(dbDriver);
    }

    config.setMigrationConfig(migrationConfig);

    config.setDataSourceConfig(dsConfig);
    config.add(new UUIDIdGenerator());
    config.setRunMigration(true);

    this.ebeanServer = io.ebean.EbeanServerFactory.create(config);
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
