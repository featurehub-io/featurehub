package io.featurehub.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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
  public Future<ClientContext> build() {
    final CompletableFuture<ClientContext> build = new CompletableFuture<>();

    build.complete(this);

    return build;
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
