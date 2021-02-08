package io.featurehub.client

class TestContext extends BaseClientContext {
  private final FeatureRepository repository

  TestContext(FeatureRepository repo) {
    this.repository = repo
  }

  @Override
  ClientContext build() {
    return this
  }

  @Override
  FeatureRepository getRepository() {
    return repository
  }

  @Override
  void close() {
  }

  @Override
  boolean exists(String key) {
    return repository.exists(key)
  }
}
