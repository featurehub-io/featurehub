package io.featurehub.edge;

import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.features.EdgeConcurrentRequestPool;
import io.featurehub.edge.features.DachaRequestOrchestrator;
import io.featurehub.edge.features.DachaFeatureRequestSubmitter;
import io.featurehub.edge.permission.PermissionPublishDelivery;
import io.featurehub.edge.permission.PermissionPublisher;
import io.featurehub.edge.stats.StatsFeature;
import io.featurehub.edge.utils.UpdateFeatureMapper;
import io.featurehub.edge.utils.UpdateMapper;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;

public class EdgeFeature implements Feature {
  @Override
  public boolean configure(FeatureContext context) {
    context
        .register(StatsFeature.class)
        .register(
            new AbstractBinder() {

              @Override
              protected void configure() {
                bind(StreamingFeatureSource.class).to(StreamingFeatureController.class).in(Singleton.class);
                bind(EventOutputBucketService.class)
                    .to(EventOutputBucketService.class)
                    .in(Singleton.class);
                bind(FeatureTransformerUtils.class)
                    .to(FeatureTransformer.class)
                    .in(Singleton.class);
                bind(ConcurrentRequestPool.class)
                    .to(EdgeConcurrentRequestPool.class)
                    .in(Singleton.class);
                bind(DachaRequestOrchestrator.class)
                    .to(DachaFeatureRequestSubmitter.class)
                    .in(Singleton.class);
                bind(UpdateFeatureMapper.class)
                  .to(UpdateMapper.class)
                  .in(Singleton.class);
                bind(PermissionPublishDelivery.class)
                  .to(PermissionPublisher.class)
                  .in(Singleton.class);
              }
            });
    return true;
  }
}
