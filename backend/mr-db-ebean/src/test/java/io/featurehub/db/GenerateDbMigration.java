package io.featurehub.db;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;

public class GenerateDbMigration {
  public static void main(String[] args) throws Exception {
    DbMigration dbMigration = DbMigration.create();
//    dbMigration.addPlatform(Platform.SQLSERVER17, "mssql");
    dbMigration.addPlatform(Platform.POSTGRES, "postgres");
    dbMigration.addPlatform(Platform.MYSQL, "mysql");
    dbMigration.addPlatform(Platform.MARIADB, "mariadb");
    dbMigration.addPlatform(Platform.H2, "h2");
    dbMigration.addPlatform(Platform.ORACLE, "oracle");

    dbMigration.setMigrationPath("/dbmigration");
    dbMigration.setStrictMode(false);

    dbMigration.setGeneratePendingDrop("1.33");
    // generate the migration ddl and xml
    dbMigration.generateMigration();
    // use this if you wish to introduce another database - it will generate it under dbinit/database with the
    // id of the latest migration.
//    dbMigration.generateInitMigration();
  }
}
