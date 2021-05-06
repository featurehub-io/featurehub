package io.featurehub.dacha;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.jersey.common.CommonConfiguration;
import cd.connect.jersey.common.InfrastructureConfiguration;
import cd.connect.jersey.common.LoggingConfiguration;
import cd.connect.jersey.common.TracingConfiguration;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;
import io.prometheus.client.hotspot.DefaultExports;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  /**
   * initialises and starts the dacha cache layer.
   */
  public static void initializeDacha() {

    final InMemoryCache inMemoryCache = new InMemoryCache();
    final ServerConfig serverConfig = new ServerConfig(inMemoryCache);
    CacheManager cm = new CacheManager(inMemoryCache, serverConfig);
    cm.init();
  }

  private static void initializeCommonJerseyLayer() throws Exception {

    // turn on all jvm prometheus metrics
    DefaultExports.initialize();

    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(
      ClientTracingFeature.class,
      CommonConfiguration.class,
      LoggingConfiguration.class,
      TracingConfiguration.class,
      InfrastructureConfiguration.class);

    new JerseyHttp2Server().start(config);

    log.info("Dacha Launched - (HTTP/2 payloads enabled!)");
  }

  public static void main(String[] args) {
    try {
      initializeCommonJerseyLayer();
      initializeDacha();
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
