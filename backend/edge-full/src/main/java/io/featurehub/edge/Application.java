package io.featurehub.edge;

import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.dacha.api.DachaClientFeature;
import io.featurehub.dacha.api.DachaClientServiceRegistry;
import io.featurehub.events.CloudEventsFeature;
import io.featurehub.events.pubsub.GoogleEventFeature;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.FeatureHubJerseyHost;
import io.featurehub.lifecycle.LifecycleListener;
import io.featurehub.lifecycle.LifecycleListeners;
import io.featurehub.publish.NATSFeature;
import io.featurehub.rest.CorsFilter;
import io.featurehub.utils.FallbackPropertyConfig;
import io.features.webhooks.features.WebhookFeature;
import io.featurehub.encryption.WebhookEncryptionFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import static io.featurehub.rest.Info.APPLICATION_NAME_PROPERTY;

public class Application {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Application.class);

  public void run() throws Exception {
    DeclaredConfigResolver.resolve(this);

    // we do not want telemetry enabled on Edge
    ResourceConfig config =
        new ResourceConfig(
            DachaClientFeature.class,
            EdgeFeature.class,
            CloudEventsFeature.class,
            GoogleEventFeature.class,
            NATSFeature.class,
            EdgeResourceFeature.class,
            WebhookEncryptionFeature.class,
            CorsFilter.class);

    if (WebhookFeature.Companion.getEnabled()) {
      config.register(WebhookFeature.class);
    }

      // check if we should list on a different port
    MetricsHealthRegistration.Companion.registerMetrics(config);
    if (FallbackPropertyConfig.Companion.getConfig("cache.name") != null) {
      LifecycleListeners.Companion.starter(ConfigureCacheApiLoader.class, config);
    }

    // this has a default grace period of 10 seconds
    new FeatureHubJerseyHost(config).disallowWebHosting().start();

    log.info("FeatureHub SSE-Edge Has Started.");

    Thread.currentThread().join();
  }

  public static void main(String[] args) {
    System.setProperty("user.timezone", "UTC");
    System.setProperty(APPLICATION_NAME_PROPERTY, "edge-full");

    try {
      new Application().run();
    } catch (Exception e) {
      log.error("Failed to start.", e);
      System.exit(-1);
    }
  }
}
