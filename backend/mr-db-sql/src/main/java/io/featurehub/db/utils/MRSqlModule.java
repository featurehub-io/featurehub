package io.featurehub.db.utils;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.ebean.Database;
import io.ebeaninternal.server.core.DefaultServer;
import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PersonApi;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.db.api.SetupApi;
import io.featurehub.db.listener.FeatureUpdateBySDKApi;
import io.featurehub.db.services.ApplicationSqlApi;
import io.featurehub.db.services.ArchiveStrategy;
import io.featurehub.db.services.AuthenticationSqlApi;
import io.featurehub.db.services.ConvertUtils;
import io.featurehub.db.services.DbArchiveStrategy;
import io.featurehub.db.services.EnvironmentSqlApi;
import io.featurehub.db.services.FeatureSqlApi;
import io.featurehub.db.services.GroupSqlApi;
import io.featurehub.db.services.OrganizationSqlApi;
import io.featurehub.db.services.PersonSqlApi;
import io.featurehub.db.services.PortfolioSqlApi;
import io.featurehub.db.services.ServiceAccountSqlApi;
import io.featurehub.db.services.SetupSqlApi;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 * Use this module to configure the EbeanSource that you can use elsewhere.
 */
public class MRSqlModule extends AbstractBinder {
  private static final Logger log = LoggerFactory.getLogger(MRSqlModule.class);
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

    bind(AuthenticationSqlApi.class).to(AuthenticationApi.class).in(Singleton.class);
    bind(SetupSqlApi.class).to(SetupApi.class).in(Singleton.class);
    bind(OrganizationSqlApi.class).to(OrganizationApi.class).in(Singleton.class);
    bind(PortfolioSqlApi.class).to(PortfolioApi.class).in(Singleton.class);
    bind(GroupSqlApi.class).to(GroupApi.class).in(Singleton.class);
    bind(ConvertUtils.class).to(ConvertUtils.class).in(Singleton.class);
    bind(PersonSqlApi.class).to(PersonApi.class).in(Singleton.class);
    bind(ServiceAccountSqlApi.class).to(ServiceAccountApi.class).in(Singleton.class);
    bind(ApplicationSqlApi.class).to(ApplicationApi.class).in(Singleton.class);
    bind(EnvironmentSqlApi.class).to(EnvironmentApi.class).in(Singleton.class);
    bind(FeatureSqlApi.class).to(FeatureApi.class).to(FeatureUpdateBySDKApi.class).in(Singleton.class);
    bind(DbArchiveStrategy.class).to(ArchiveStrategy.class).in(Singleton.class);
  }
}
