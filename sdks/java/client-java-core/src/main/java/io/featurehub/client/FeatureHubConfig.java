package io.featurehub.client;

public interface FeatureHubConfig {
  /**
   * What is the fully deconstructed URL for the server?
   */
  String getUrl();

  String sdkKey();

  String baseUrl();

  /**
   * The SDK URL indicates this is going to be server based evaluation
   */
  boolean isServerEvaluation();

  static boolean sdkKeyIsClientSideEvaluated(String sdkKey) {
    return sdkKey.contains("*");
  }
}
