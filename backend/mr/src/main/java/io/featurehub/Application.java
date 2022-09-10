package io.featurehub;

import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.FeatureHubJerseyHost;
import io.featurehub.lifecycle.TelemetryFeature;
import io.featurehub.mr.ManagementRepositoryFeature;
import io.featurehub.mr.dacha2.Dacha2Feature;
import io.featurehub.publish.NATSFeature;
import io.featurehub.web.security.oauth.AuthProviderCollection;
import io.featurehub.web.security.oauth.AuthProviders;
import io.featurehub.web.security.oauth.NoAuthProviders;
import io.featurehub.web.security.oauth.OAuth2Feature;
import io.featurehub.web.security.saml.SamlEnvironmentalFeature;
import jakarta.inject.Singleton;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.featurehub.rest.Info.APPLICATION_NAME_PROPERTY;

public class Application {
  private static final Logger log = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    System.setProperty(APPLICATION_NAME_PROPERTY, "management-repository");

    try {
      new Application().run();
    } catch (Exception e) {
      log.error("failed", e);
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATING);
      System.exit(-1);
    }
  }

  private void run() throws Exception {
    // register our resources, try and tag them as singleton as they are instantiated faster
    ResourceConfig config = new ResourceConfig(
      ManagementRepositoryFeature.class,
      SamlEnvironmentalFeature.class,
      NATSFeature.class,
      TelemetryFeature.class
      );

    MetricsHealthRegistration.Companion.registerMetrics(config, (resourceConfig) -> {
      resourceConfig.register(Dacha2Feature.class);

      return resourceConfig;
    });

    new FeatureHubJerseyHost(config).start();

    log.info("MR Launched - (HTTP/2 payloads enabled!)");

    Thread.currentThread().join();
  }
}
