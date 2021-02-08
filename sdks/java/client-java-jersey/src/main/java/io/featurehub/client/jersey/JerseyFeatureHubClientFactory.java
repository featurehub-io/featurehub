package io.featurehub.client.jersey;

import io.featurehub.client.EdgeService;
import io.featurehub.client.FeatureHubClientFactory;
import io.featurehub.client.FeatureHubConfig;
import io.featurehub.client.FeatureStore;

public class JerseyFeatureHubClientFactory implements FeatureHubClientFactory {
  @Override
  public EdgeService createEdgeService(FeatureHubConfig url, FeatureStore repository) {
    return new JerseyClient(url, repository);
  }
}
