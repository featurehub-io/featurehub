package io.featurehub.client;

public interface FeatureHubClientFactory {
  /**
   * allows the creation of a new edge service without knowing about the underlying implementation.
   * depending on which library is included, this will automatically be created.
   *
   * @param url - the full edge url
   * @return
   */
  EdgeService createEdgeService(FeatureHubConfig url, FeatureStore repository);
}
