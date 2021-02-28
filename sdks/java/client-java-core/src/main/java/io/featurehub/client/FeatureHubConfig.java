package io.featurehub.client;

import java.util.function.Supplier;

public interface FeatureHubConfig {
  /**
   * What is the fully deconstructed URL for the server?
   */
  String getRealtimeUrl();

  String sdkKey();

  String baseUrl();

  /**
   * The SDK URL indicates this is going to be server based evaluation
   */
  boolean isServerEvaluation();

  ClientContext newContext();
  ClientContext newContext(FeatureRepositoryContext repository, Supplier<EdgeService> edgeService);

  static boolean sdkKeyIsClientSideEvaluated(String sdkKey) {
    return sdkKey.contains("*");
  }

  void setRepository(FeatureRepositoryContext repository);
  FeatureRepositoryContext getRepository();

  void setEdgeService(EdgeService edgeService);
  EdgeService getEdgeService();

}
