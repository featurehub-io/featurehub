package io.featurehub.dacha;

import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.FeatureHubJerseyHost;
import io.featurehub.lifecycle.TelemetryFeature;
import io.featurehub.publish.NATSFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.featurehub.rest.Info.APPLICATION_NAME_PROPERTY;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static void initializeCommonJerseyLayer() {
    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config =
        new ResourceConfig(
                NATSFeature.class,
                TelemetryFeature.class,
                DachaFeature.class);

    // check if we should list on a different port
    MetricsHealthRegistration.Companion.registerMetrics(config);

    new FeatureHubJerseyHost(config).disallowWebHosting().start();

    log.info("Dacha Launched - (HTTP/2 payloads enabled!)");
  }

  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    System.setProperty(APPLICATION_NAME_PROPERTY, "dacha");

    try {
      initializeCommonJerseyLayer();
      log.info("Cache has started");

      Thread.currentThread().join();
    } catch (Exception e) {
      log.error("Failed to start", e);
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATING);
      System.exit(-1);
    }
  }
}
