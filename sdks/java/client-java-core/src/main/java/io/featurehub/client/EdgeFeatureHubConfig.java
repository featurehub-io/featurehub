package io.featurehub.client;

public class EdgeFeatureHubConfig implements FeatureHubConfig {
  private final String url;
  private final boolean serverEvaluation;
  private final String edgeUrl;
  private final String sdkKey;

  public EdgeFeatureHubConfig(String edgeUrl, String sdkKey) {

    if (sdkKey == null || edgeUrl == null) {
      throw new RuntimeException("Both edge url and sdk key must be set.");
    }

    serverEvaluation = !FeatureHubConfig.sdkKeyIsClientSideEvaluated(sdkKey);

    if (edgeUrl.endsWith("/")) {
      edgeUrl = edgeUrl.substring(0, edgeUrl.length()-1);
    }

    if (edgeUrl.endsWith("/features")) {
      edgeUrl = edgeUrl.substring(0, edgeUrl.length() - "/features".length());
    }

    this.edgeUrl = String.format("%s", edgeUrl);
    this.sdkKey = sdkKey;

    url = String.format("%s/features/%s", edgeUrl, sdkKey);
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String sdkKey() {
    return sdkKey;
  }

  @Override
  public String baseUrl() {
    return edgeUrl;
  }

  @Override
  public boolean isServerEvaluation() {
    return serverEvaluation;
  }
}
