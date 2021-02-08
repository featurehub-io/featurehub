package io.featurehub.client;

import java.util.ServiceLoader;

public class FeatureContext extends BaseClientContext {
  private final FeatureRepositoryContext repository;
  private EdgeService edgeService;
  private FeatureHubConfig url;

  public FeatureContext(FeatureHubConfig url) {
    this.repository = new ClientFeatureRepository();
    this.url = url;
  }

  public FeatureContext(FeatureRepositoryContext repository, EdgeService edgeService) {
    this.repository = repository;
    this.edgeService = edgeService;
  }

  @Override
  public ClientContext build() {
    if (edgeService == null) {
      this.edgeService = loadEdgeService(url);
    }

    if (edgeService != null) {
      edgeService.contextChange(context());
    }

    return this;
  }

  @Override
  public FeatureState feature(String name) {
    if (repository.isServerEvaluation()) {
      return repository.getFeatureState(name);
    } else {
      return repository.getFeatureState(name).withContext(this);
    }
  }

  @Override
  public FeatureRepository getRepository() {
    return repository;
  }

  /**
   * dynamically load an edge service implementation
   */
  private EdgeService loadEdgeService(FeatureHubConfig url) {
    ServiceLoader<FeatureHubClientFactory> loader = ServiceLoader.load(FeatureHubClientFactory.class);

    for(FeatureHubClientFactory f : loader) {
      EdgeService edgeService = f.createEdgeService(url, repository);
      if (edgeService != null) {
        return edgeService;
      }
    }

    return null;
  }

  @Override
  public boolean isEnabled(String name) {
    return feature(name).getBoolean() == Boolean.TRUE;
  }

  @Override
  public void close() {
    if (edgeService != null) {
      edgeService.close();
    }
  }

  @Override
  public boolean exists(String key) {
    return repository.exists(key);
  }


}
