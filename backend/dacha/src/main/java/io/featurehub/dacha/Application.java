package io.featurehub.dacha;

import cd.connect.jersey.JerseyHttp2Server;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.health.CommonFeatureHubFeatures;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.config.EndpointLoggingListener;
import io.featurehub.publish.NATSFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static void initializeCommonJerseyLayer() throws Exception {
    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config =
        new ResourceConfig(
                CommonFeatureHubFeatures.class,
                EndpointLoggingListener.class,
                NATSFeature.class,
                DachaFeature.class);

    // check if we should list on a different port
    MetricsHealthRegistration.Companion.registerMetrics(config);

    new JerseyHttp2Server().start(config);

    log.info("Dacha Launched - (HTTP/2 payloads enabled!)");
  }

  public static void main(String[] args) {
    try {
      initializeCommonJerseyLayer();
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
