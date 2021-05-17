package io.featurehub.edge;

import com.lmax.disruptor.EventHandler;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.rest.EventStreamResource;
import io.featurehub.edge.rest.SSEHeaderFilter;
import io.featurehub.edge.stats.NATSStatPublisher;
import io.featurehub.edge.stats.Stat;
import io.featurehub.edge.stats.StatCollector;
import io.featurehub.edge.stats.StatDisruptor;
import io.featurehub.edge.stats.StatsCollectionOrchestrator;
import io.featurehub.edge.stats.StatEventHandler;
import io.featurehub.edge.stats.StatPublisher;
import io.featurehub.edge.stats.StatRecorder;
import io.featurehub.edge.stats.StatTimeTrigger;
import io.featurehub.edge.stats.StatsOrchestrator;
import io.featurehub.publish.NATSSource;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.inject.Singleton;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.GenericType;

public class EdgeFeature implements Feature {

  private final static GenericType<EventHandler<Stat>> eventHandlerType = new GenericType<>(){};

  @Override
  public boolean configure(FeatureContext context) {
    context
        .register(EventStreamResource.class)
        .register(SSEHeaderFilter.class)
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
                bind(StatsCollectionOrchestrator.class)
                    .to(StatsOrchestrator.class)
                    .in(Singleton.class);
                bind(StatDisruptor.class).to(StatRecorder.class).in(Singleton.class);
                bind(NATSStatPublisher.class).to(StatPublisher.class).in(Singleton.class);
                bind(StatsCollectionOrchestrator.class).to(StatsOrchestrator.class).in(Singleton.class);
                bind(StatEventHandler.class).to(StatCollector.class).to(eventHandlerType).in(Singleton.class);
                bind(StatTimeTrigger.class).to(StatTimeTrigger.class).in(Singleton.class);
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

                // starts the stats time publisher if there are any
                injector.getService(StatTimeTrigger.class);
                injector.getService(StatRecorder.class);
                System.out.println("printing");
              }

              public void onReload(Container container) {}

              public void onShutdown(Container container) {
                /*...*/
              }
            });
    return true;
  }
}
