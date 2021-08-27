package io.featurehub.db.utils;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.ebean.Database;
import io.ebeaninternal.server.core.DefaultServer;
import io.featurehub.health.HealthSource;
import jakarta.inject.Singleton;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use this module to configure the EbeanSource that you can use elsewhere.
 */
public class DatabaseBinder extends AbstractBinder {
  private static final Logger log = LoggerFactory.getLogger(DatabaseBinder.class);
  @ConfigKey("db.url")
  String databaseUrl;
  @ConfigKey("db.username")
  String username;
  @ConfigKey("db.password")
  String password;
  @ConfigKey("db.driver")
  String driver = "";
  @ConfigKey("db.connections")
  Integer maxConnections;

  @Override
  protected void configure() {
    DeclaredConfigResolver.resolve(this);

    if (this.driver.length() == 0) {
      this.driver = null;
    }

    EbeanHolder ebeanHolder = new EbeanHolder(this.databaseUrl, this.username, this.password, this.maxConnections, this.driver);

    log.info("database platform initialised: {}", ((DefaultServer) ebeanHolder.getEbeanServer()).getDatabasePlatform().getName());

    bind(ebeanHolder).to(EbeanSource.class).in(Singleton.class);
    bind(ebeanHolder.getEbeanServer()).to(Database.class).in(Singleton.class);
    bind(DatabaseHealthSource.class).to(HealthSource.class).in(Singleton.class);
  }
}
