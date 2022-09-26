package io.featurehub.edge;

import io.featurehub.edge.bucket.BucketService;
import io.featurehub.edge.bucket.EventOutputBucketService;
import io.featurehub.edge.events.CloudEventsFeatureUpdatePublisherImpl;
import io.featurehub.edge.events.EdgeSubscriber;
import io.featurehub.edge.events.EdgeSubscriberImpl;
import io.featurehub.edge.events.kinesis.KinesisEdgeFeature;
import io.featurehub.edge.events.nats.NatsEdgeFeature;
import io.featurehub.edge.events.pubsub.PubsubEdgeFeature;
import io.featurehub.edge.rest.FeatureSse;
import io.featurehub.edge.rest.FeatureSseProcessor;
import io.featurehub.edge.rest.FeatureUpdatePublisher;
import io.featurehub.edge.stats.StatsFeature;
import io.featurehub.events.CloudEventsFeature;
import io.featurehub.lifecycle.TelemetryFeature;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.inject.AbstractBinder;

/**
 * This one is used by Edge REST. EdgeCommonFeature is shared with edge-get.
 */
public class EdgeFeature implements Feature {
  @Override
  public boolean configure(FeatureContext context) {
    context.register(TelemetryFeature.class);
    context.register(CloudEventsFeature.class);
    context
        .register(StatsFeature.class)
        .register(EdgeCommonFeature.class)
        .register(
            new AbstractBinder() {

              @Override
              protected void configure() {
                bind(StreamingFeatureSource.class).to(StreamingFeatureController.class).in(Singleton.class);
                bind(FeatureSseProcessor.class).to(FeatureSse.class).in(Singleton.class);
                bind(EdgeSubscriberImpl.class).to(EdgeSubscriber.class).in(Singleton.class);
                bind(EventOutputBucketService.class)
                    .to(BucketService.class)
                    .in(Singleton.class);
                bind(CloudEventsFeatureUpdatePublisherImpl.class)
                  .to(FeatureUpdatePublisher.class)
                  .in(Singleton.class);
              }
            });

    context.register(NatsEdgeFeature.class);
    context.register(PubsubEdgeFeature.class);
    context.register(KinesisEdgeFeature.class);

    return true;
  }
}
