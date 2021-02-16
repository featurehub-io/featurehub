package io.featurehub.client

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class TestContext extends BaseClientContext {
  TestContext(FeatureRepositoryContext repo) {
    super(repo, null)
  }

  @Override
  Future<ClientContext> build() {
    CompletableFuture<ClientContext> x = CompletableFuture<ClientContext>()
    x.complete(this)
    return x
  }

  @Override
  EdgeService getEdgeService() {
    return null
  }

  @Override
  void close() {
  }

  @Override
  boolean exists(String key) {
    return repository.exists(key)
  }
}
