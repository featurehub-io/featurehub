package io.featurehub.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ServerEvalFeatureContext extends BaseClientContext {
  private static final Logger log = LoggerFactory.getLogger(ServerEvalFeatureContext.class);
  private final Supplier<EdgeService> edgeService;
  private EdgeService currentEdgeService;
  private String xHeader;
  private final boolean weOwnRepositoryAndEdge;

  public ServerEvalFeatureContext(FeatureHubConfig config, FeatureRepositoryContext repository,
                                  Supplier<EdgeService> edgeService) {
    super(repository, config);

    this.edgeService = edgeService;
    this.weOwnRepositoryAndEdge = false;
  }

  @Override
  public Future<ClientContext> build() {
    String newHeader = FeatureStateUtils.generateXFeatureHubHeaderFromMap(clientContext);

    if (newHeader != null || xHeader != null) {
      if ((newHeader != null && xHeader == null) || newHeader == null || !newHeader.equals(xHeader)) {
        xHeader = newHeader;

        repository.notReady();

        if (currentEdgeService != null && currentEdgeService.isRequiresReplacementOnHeaderChange()) {
          currentEdgeService.close();
          currentEdgeService = edgeService.get();
        }
      }
    }

    if (currentEdgeService == null) {
      currentEdgeService = edgeService.get();
    }

    Future<?> change = currentEdgeService.contextChange(newHeader);

    xHeader = newHeader;

    CompletableFuture<ClientContext> future = new CompletableFuture<>();

    try {
      change.get();
    } catch (Exception e) {
      log.error("Failed to update", e);
    }

    future.complete(this);

    return future;
  }

  @Override
  public EdgeService getEdgeService() {
    return currentEdgeService;
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
