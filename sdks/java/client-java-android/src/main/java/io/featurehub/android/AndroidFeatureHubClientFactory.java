package io.featurehub.android;

import io.featurehub.client.EdgeService;
import io.featurehub.client.FeatureHubClientFactory;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStore;

import java.util.Arrays;

public class AndroidFeatureHubClientFactory implements FeatureHubClientFactory {
  @Override
  public EdgeService createEdgeService(FeatureHubConfig url, FeatureStore repository) {
    return new FeatureHubClient(url.baseUrl(), Arrays.asList(url.sdkKey()), repository);
  }
}
