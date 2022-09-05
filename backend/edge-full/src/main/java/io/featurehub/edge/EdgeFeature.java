package io.featurehub.edge;

import io.featurehub.edge.bucket.BucketService;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.permission.PermissionPublishDelivery;
import io.featurehub.edge.rest.FeatureSse;
import io.featurehub.edge.rest.FeatureSseProcessor;
import io.featurehub.edge.rest.FeatureUpdatePublisher;
import io.featurehub.edge.stats.StatsFeature;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;

public class EdgeFeature implements Feature {
  @Override
  public boolean configure(FeatureContext context) {
    context
        .register(StatsFeature.class)
        .register(EdgeCommonFeature.class)
        .register(
            new AbstractBinder() {

              @Override
              protected void configure() {
                bind(StreamingFeatureSource.class).to(StreamingFeatureController.class).in(Singleton.class);
                bind(FeatureSseProcessor.class).to(FeatureSse.class).in(Singleton.class);
                bind(EventOutputBucketService.class)
                    .to(BucketService.class)
                    .in(Singleton.class);
                bind(PermissionPublishDelivery.class)
                  .to(FeatureUpdatePublisher.class)
                  .in(Singleton.class);
              }
            });
    return true;
  }
}
