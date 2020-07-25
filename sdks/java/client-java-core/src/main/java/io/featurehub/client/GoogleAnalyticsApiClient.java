package io.featurehub.client;

public interface GoogleAnalyticsApiClient {
  // if you wish to pass in the "value" field to analytics, add this to the "other" map
  String GA_VALUE = "gaValue";

  void postBatchUpdate(String batchData);
}
