package io.featurehub.client;

/**
 * This class is ONLY used when we are doing client side evaluation. So the edge service stays the same.
 */
class ClientEvalFeatureContext extends BaseClientContext {
  private final EdgeService edgeService;
  private final boolean weOwnRepositoryAndEdge;

  public ClientEvalFeatureContext(FeatureHubConfig config) {
    super(new ClientFeatureRepository(), config);

    this.edgeService = loadEdgeService(config, repository).get();
    this.weOwnRepositoryAndEdge = true;
  }

  public ClientEvalFeatureContext(FeatureHubConfig config, FeatureRepositoryContext repository,
                                  EdgeService edgeService) {
    super(repository, config);

    this.edgeService = edgeService;
    this.weOwnRepositoryAndEdge = false;
  }

  // this doesn't matter for client eval
  @Override
  public ClientContext build() {
    return this;
  }

  @Override
  public FeatureState feature(String name) {
    return repository.getFeatureState(name).withContext(this);
  }

  @Override
  public EdgeService getEdgeService() {
    return edgeService;
  }

  /**
   * We only close if we created the edge and repository
   */
  @Override
  public void close() {
    if (weOwnRepositoryAndEdge) {
      repository.close();
      edgeService.close();
    }
  }
}
