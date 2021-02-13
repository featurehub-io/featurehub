package io.featurehub.client

class TestContext extends BaseClientContext {
  TestContext(FeatureRepositoryContext repo) {
    super(repo, null)
  }

  @Override
  ClientContext build() {
    return this
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
