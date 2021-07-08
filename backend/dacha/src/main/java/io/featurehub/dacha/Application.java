package io.featurehub.dacha;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.health.CommonFeatureHubFeatures;
import io.featurehub.health.HealthSource;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.publish.NATSHealthSource;
import io.featurehub.publish.NATSSource;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Singleton;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static ServerConfig serverConfig;

  /**
   * initialises and starts the dacha cache layer.
   */
  public static CacheManager initializeDacha() {
    final InMemoryCache inMemoryCache = new InMemoryCache();
    serverConfig = new ServerConfig(inMemoryCache);
    CacheManager cm = new CacheManager(inMemoryCache, serverConfig);
    cm.init();

    return cm;
  }

  private static void initializeCommonJerseyLayer(CacheManager cm) throws Exception {
    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(CommonFeatureHubFeatures.class)
      .register(new AbstractBinder() {
        @Override
        protected void configure() {
          bind(serverConfig).to(NATSSource.class).in(Singleton.class);
          bind(cm).to(HealthSource.class).in(Singleton.class);
          bind(NATSHealthSource.class).to(HealthSource.class).in(Singleton.class);
        }
      });

    // check if we should list on a different port
    MetricsHealthRegistration.Companion.registerMetrics(config);

    new JerseyHttp2Server().start(config);

    log.info("Dacha Launched - (HTTP/2 payloads enabled!)");
  }

  public static void main(String[] args) {
    try {
      initializeCommonJerseyLayer(initializeDacha());
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED);

      log.info("Cache has started");

      Thread.currentThread().join();
    } catch (Exception e) {
      log.error("Failed to start", e);
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATED);
      System.exit(-1);
    }
  }
}
