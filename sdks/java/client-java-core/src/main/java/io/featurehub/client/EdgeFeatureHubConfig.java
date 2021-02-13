package io.featurehub.client;

import java.util.function.Supplier;

public class EdgeFeatureHubConfig implements FeatureHubConfig {
  private final String realtimeUrl;
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

    realtimeUrl = String.format("%s/features/%s", edgeUrl, sdkKey);
  }

  @Override
  public String getRealtimeUrl() {
    return realtimeUrl;
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

  @Override
  public ClientContext newContext() {
    if (isServerEvaluation()) {
      return new ServerEvalFeatureContext(this);
    }

    return new ClientEvalFeatureContext(this);
  }

  @Override
  public ClientContext newContext(FeatureRepositoryContext repository, Supplier<EdgeService> edgeService) {
    if (isServerEvaluation()) {
      return new ServerEvalFeatureContext(this, repository, edgeService);
    }

    return new ClientEvalFeatureContext(this, repository, edgeService.get());
  }
}
