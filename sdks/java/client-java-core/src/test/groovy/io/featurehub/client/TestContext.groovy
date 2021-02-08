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
  FeatureState feature(String name) {
    return repository.getFeatureState(name)
  }

  @Override
  FeatureRepository getRepository() {
    return repository
  }

  @Override
  boolean isEnabled(String name) {
    return repository.isEnabled(name);
  }

  @Override
  void close() {
  }

  @Override
  boolean exists(String key) {
    return repository.exists(key)
  }
}
