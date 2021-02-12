package io.featurehub.client;

import java.util.ServiceLoader;

public class FeatureContext extends BaseClientContext {
  private final FeatureRepositoryContext repository;
  private EdgeService edgeService;
  private FeatureHubConfig url;

  public FeatureContext(FeatureHubConfig url) {
    this.repository = new ClientFeatureRepository();
    this.url = url;
    this.edgeService = loadEdgeService(url);
  }

  public FeatureContext(FeatureRepositoryContext repository, EdgeService edgeService) {
    this.repository = repository;
    this.edgeService = edgeService;
  }

  @Override
  public ClientContext build() {
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

  @Override
  public EdgeService getEdgeService() {
    return edgeService;
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
  public boolean exists(String key) {
    return repository.exists(key);
  }

  /**
   * Only use this if this is the _only_ Context, otherwise all contexts will invalidate.
   */
  @Override
  public void close() {
    repository.close();
    edgeService.close();
  }


}
