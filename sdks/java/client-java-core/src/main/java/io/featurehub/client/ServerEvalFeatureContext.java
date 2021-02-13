package io.featurehub.client;

import java.util.function.Supplier;

public class ServerEvalFeatureContext extends BaseClientContext {
  private final Supplier<EdgeService> edgeService;
  private EdgeService currentEdgeService;
  private String xHeader;
  private final boolean weOwnRepositoryAndEdge;

  public ServerEvalFeatureContext(FeatureHubConfig config) {
    super(new ClientFeatureRepository(), config);

    this.edgeService = loadEdgeService(config, repository);
    this.weOwnRepositoryAndEdge = true;
  }

  public ServerEvalFeatureContext(FeatureHubConfig config, FeatureRepositoryContext repository,
                                  Supplier<EdgeService> edgeService) {
    super(repository, config);

    this.edgeService = edgeService;
    this.weOwnRepositoryAndEdge = false;
  }

  @Override
  public ClientContext build() {
    String newHeader = FeatureStateUtils.generateXFeatureHubHeaderFromMap(clientContext);

    if (!newHeader.equals(xHeader)) {
      xHeader = newHeader;

      repository.notReady();

      if (currentEdgeService != null && currentEdgeService.isRequiresReplacementOnHeaderChange()) {
        currentEdgeService.close();
        currentEdgeService = edgeService.get();
      }
    }

    if (currentEdgeService == null) {
      currentEdgeService = edgeService.get();
    }

    currentEdgeService.contextChange(newHeader);

    xHeader = newHeader;

    return this;
  }

  @Override
  public EdgeService getEdgeService() {
    return null;
  }

  @Override
  public boolean exists(String key) {
    return false;
  }

  @Override
  public void close() {
    if (weOwnRepositoryAndEdge) {
      repository.close();

      if (currentEdgeService != null) {
        currentEdgeService.close();
      }
    }
  }
}
