package io.featurehub;

import static io.featurehub.rest.Info.APPLICATION_NAME_PROPERTY;

import io.featurehub.events.CloudEventConfigDiscoveryService;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.FeatureHubJerseyHost;
import io.featurehub.lifecycle.ApplicationLifecycleManager;
import io.featurehub.lifecycle.LifecycleStatus;
import io.featurehub.lifecycle.TelemetryFeature;
import io.featurehub.mr.ManagementRepositoryFeature;
import io.featurehub.mr.dacha2.Dacha2Feature;
import io.featurehub.mr.events.common.CacheSource;
import io.featurehub.mr.events.redis.RedisFullSync;
import io.featurehub.mr.maintenance.MaintenanceCachePublisher;
import io.featurehub.mr.maintenance.MaintenancePublishResource;
import io.featurehub.mr.maintenance.MaintenancePublisher;
import io.featurehub.mr.events.dacha2.CacheApi;
import io.featurehub.mr.utils.ConfigurationUtils;
import io.featurehub.web.security.saml.SamlEnvironmentalFeature;
import jakarta.inject.Singleton;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    System.setProperty(APPLICATION_NAME_PROPERTY, "management-repository");
    CloudEventConfigDiscoveryService.Companion.addTags("mr");
    CloudEventConfigDiscoveryService.Companion.addTags(ConfigurationUtils.Companion.getDacha1Enabled() ? "mr-dacha1" : "mr-dacha2");

    try {
      new Application().run();
    } catch (Exception e) {
      log.error("failed", e);
      // force any running Jersey context's to terminate
      ApplicationLifecycleManager.Companion.updateStatus(LifecycleStatus.TERMINATING);
      System.exit(-1);
    }
  }

  private void run() throws Exception {
    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(
      ManagementRepositoryFeature.class,
      SamlEnvironmentalFeature.class,
      TelemetryFeature.class
      );

    // the maintenance publish-all endpoint is ONLY ever registered on the separate (internal) monitoring port, never on
    // the main port, as it is not authenticated. It is only reachable from inside the cluster (e.g. kubectl port-forward).
    final boolean maintenanceEndpoint = MetricsHealthRegistration.Companion.hasExternalMonitoringPort();

    MetricsHealthRegistration.Companion.registerMetrics(config, (resourceConfig, locator, binder) -> {
      if (locator != null) {
        binder.bind(locator.getService(CacheApi.class)).to(CacheApi.class).in(Singleton.class);

        if (maintenanceEndpoint) {
          // either may be missing (cache publishing disabled, redis not configured)
          binder.bind(new MaintenancePublisher(locator.getService(CacheSource.class), locator.getService(RedisFullSync.class)))
            .to(MaintenanceCachePublisher.class);
        }
      }

      if (resourceConfig != null) {
        resourceConfig.register(Dacha2Feature.class);

        if (maintenanceEndpoint) {
          resourceConfig.register(MaintenancePublishResource.class);
        }
      }

      return resourceConfig;
    });

    new FeatureHubJerseyHost(config).start();

    log.info("MR Launched - (HTTP/2 payloads enabled!)");

    Thread.currentThread().join();
  }
}
