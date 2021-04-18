package io.featurehub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class EdgeFeatureHubConfig implements FeatureHubConfig {
  private static final Logger log = LoggerFactory.getLogger(EdgeFeatureHubConfig.class);
  private final String realtimeUrl;
  private final boolean serverEvaluation;
  private final String edgeUrl;
  private final String apiKey;
  private FeatureRepositoryContext repository;
  private Supplier<EdgeService> edgeService;

  public EdgeFeatureHubConfig(String edgeUrl, String apiKey) {

    if (apiKey == null || edgeUrl == null) {
      throw new RuntimeException("Both edge url and sdk key must be set.");
    }

    serverEvaluation = !FeatureHubConfig.sdkKeyIsClientSideEvaluated(apiKey);

    if (edgeUrl.endsWith("/")) {
      edgeUrl = edgeUrl.substring(0, edgeUrl.length()-1);
    }

    if (edgeUrl.endsWith("/features")) {
      edgeUrl = edgeUrl.substring(0, edgeUrl.length() - "/features".length());
    }

    this.edgeUrl = String.format("%s", edgeUrl);
    this.apiKey = apiKey;

    realtimeUrl = String.format("%s/features/%s", edgeUrl, apiKey);
  }

  @Override
  public String getRealtimeUrl() {
    return realtimeUrl;
  }

  @Override
  public String apiKey() {
    return apiKey;
  }

  @Override
  public String baseUrl() {
    return edgeUrl;
  }

  @Override
  public void init() {
    try {
      newContext().build().get();
    } catch (Exception e) {
      log.error("Failed to initialize FeatureHub client", e);
    }
  }

  @Override
  public boolean isServerEvaluation() {
    return serverEvaluation;
  }

  @Override
  public ClientContext newContext() {
    return newContext(null, null);
  }

  @Override
  public ClientContext newContext(FeatureRepositoryContext repository, Supplier<EdgeService> edgeService) {
    if (repository == null) {
      if (this.repository == null) {
        this.repository = new ClientFeatureRepository();
      }

      repository = this.repository;
    }

    if (edgeService == null) {
      if (this.edgeService == null) {
        this.edgeService = loadEdgeService(repository);
      }
    }

    if (isServerEvaluation()) {
      return new ServerEvalFeatureContext(this, repository, edgeService);
    }

    return new ClientEvalFeatureContext(this, repository, edgeService.get());
  }

  /**
   * dynamically load an edge service implementation
   */
  protected Supplier<EdgeService> loadEdgeService(FeatureRepositoryContext repository) {
    ServiceLoader<FeatureHubClientFactory> loader = ServiceLoader.load(FeatureHubClientFactory.class);

    for(FeatureHubClientFactory f : loader) {
      Supplier<EdgeService> edgeService = f.createEdgeService(this, repository);
      if (edgeService != null) {
        return edgeService;
      }
    }

    throw new RuntimeException("Unable to find an edge service for featurehub, please include one on classpath.");
  }

  @Override
  public void setRepository(FeatureRepositoryContext repository) {
    this.repository = repository;
  }

  @Override
  public FeatureRepositoryContext getRepository() {
    if (repository == null) {
      repository = new ClientFeatureRepository();
    }

    return repository;
  }

  @Override
  public void setEdgeService(Supplier<EdgeService> edgeService) {
    this.edgeService = edgeService;
  }

  @Override
  public Supplier<EdgeService> getEdgeService() {
    if (edgeService == null) {
      edgeService = loadEdgeService(getRepository());
    }

    return edgeService;
  }
}
