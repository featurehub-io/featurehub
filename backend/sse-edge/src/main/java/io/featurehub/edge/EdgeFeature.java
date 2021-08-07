package io.featurehub.edge;

import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.stats.StatsFeature;
import io.featurehub.publish.NATSSource;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

public class EdgeFeature implements Feature {
  @Override
  public boolean configure(FeatureContext context) {
    context
      .register(StatsFeature.class)
        .register(
            new AbstractBinder() {

              @Override
              protected void configure() {
                bind(ServerConfig.class)
                    .to(ServerConfig.class)
                    .to(NATSSource.class)
                    .named("edge-source")
                    .in(Singleton.class);
                bind(EventOutputBucketService.class)
                    .to(EventOutputBucketService.class)
                    .in(Singleton.class);
                bind(FeatureTransformerUtils.class)
                    .to(FeatureTransformer.class)
                    .in(Singleton.class);
              }
            })
        .register(
            new ContainerLifecycleListener() {
              public void onStartup(Container container) {
                // access the ServiceLocator here
                ServiceLocator injector =
                    container
                        .getApplicationHandler()
                        .getInjectionManager()
                        .getInstance(ServiceLocator.class);

                injector.getService(EventOutputBucketService.class);
                injector.getService(ServerConfig.class);
              }

              public void onReload(Container container) {}

              public void onShutdown(Container container) {
                /*...*/
              }
            });
    return true;
  }
}
