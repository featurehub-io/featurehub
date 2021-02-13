package io.featurehub.android;

import io.featurehub.client.EdgeService;
import io.featurehub.client.FeatureHubClientFactory;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStore;

import java.util.Arrays;
import java.util.function.Supplier;

public class AndroidFeatureHubClientFactory implements FeatureHubClientFactory {
  @Override
  public Supplier<EdgeService> createEdgeService(final FeatureHubConfig config, final FeatureStore repository) {
    return () -> new FeatureHubClient(config.baseUrl(), Arrays.asList(config.sdkKey()), repository, config);
  }
}
