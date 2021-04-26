package io.featurehub.client;

import java.util.function.Supplier;

public interface FeatureHubClientFactory {
  /**
   * allows the creation of a new edge service without knowing about the underlying implementation.
   * depending on which library is included, this will automatically be created.
   *
   * @param url - the full edge url
   * @return
   */
  Supplier<EdgeService> createEdgeService(FeatureHubConfig url, FeatureStore repository);
}
