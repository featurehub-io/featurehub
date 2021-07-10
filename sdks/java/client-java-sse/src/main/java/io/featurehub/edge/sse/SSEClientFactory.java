package io.featurehub.edge.sse;

import io.featurehub.client.EdgeService;
import io.featurehub.client.FeatureHubClientFactory;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStore;
import io.featurehub.client.edge.EdgeRetryer;

import java.util.function.Supplier;

public class SSEClientFactory implements FeatureHubClientFactory {
  @Override
  public Supplier<EdgeService> createEdgeService(FeatureHubConfig url, FeatureStore repository) {
    return () ->
        new SSEClient(repository, url, EdgeRetryer.EdgeRetryerBuilder.anEdgeRetrier().build());
  }
}
