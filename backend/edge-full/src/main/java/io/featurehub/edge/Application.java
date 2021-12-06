package io.featurehub.edge;

import cd.connect.app.config.DeclaredConfigResolver;
import cd.connect.jersey.common.CorsFilter;
import cd.connect.lifecycle.ApplicationLifecycleManager;
import cd.connect.lifecycle.LifecycleStatus;
import io.featurehub.dacha.api.DachaClientFeature;
import io.featurehub.dacha.api.DachaClientServiceRegistry;
import io.featurehub.health.MetricsHealthRegistration;
import io.featurehub.jersey.FeatureHubJerseyHost;
import io.featurehub.lifecycle.TelemetryFeature;
import io.featurehub.publish.NATSFeature;
import io.featurehub.utils.FallbackPropertyConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

public class Application {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Application.class);

  public void run() throws Exception {
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING);

    DeclaredConfigResolver.resolve(this);

    // we do not want telemetry enabled on Edge
    ResourceConfig config = new ResourceConfig(
      NATSFeature.class,
      DachaClientFeature.class,
      EdgeFeature.class,
      EdgeResourceFeature.class,
      CorsFilter.class
      );

    // check if we should list on a different port
    MetricsHealthRegistration.Companion.registerMetrics(config);

    if (FallbackPropertyConfig.Companion.getConfig("cache.name") != null) {
      config.register(new ContainerLifecycleListener() {
        @Override
        public void onStartup(Container container) {
          FeatureHubJerseyHost.Companion.withServiceLocator(container, (serviceLocator) -> {
            DachaClientServiceRegistry registry = serviceLocator.getService(DachaClientServiceRegistry.class);
            if (registry.getApiKeyService(FallbackPropertyConfig.Companion.getConfig("cache.name")) == null) {
              log.error("You must configure the URL indicating where dacha is located. dacha.url.{} is missing", FallbackPropertyConfig.Companion.getConfig("cache.name"));
              throw new RuntimeException("Cannot find dacha url, see error log");
            }

            return null;
          });
        }

        @Override
        public void onReload(Container container) {}

        @Override
        public void onShutdown(Container container) {}
      });
    }

    // this has a default grace period of 10 seconds
    new FeatureHubJerseyHost(config).start();

    log.info("FeatureHub SSE-Edge Has Started.");

    Thread.currentThread().join();
  }

  public static void main(String[] args) {
    try {
      new Application().run();
    } catch (Exception e) {
      log.error("Failed to start.", e);
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATING);
      System.exit(-1);
    }
  }
}
