package io.featurehub.client;

abstract public class AbstractFeatureRepository implements FeatureRepository {

  @Override
  public FeatureState getFeatureState(Feature feature) {
    return this.getFeatureState(feature.name());
  }

  @Override
  public boolean exists(Feature key) {
    return exists(key.name());
  }

  @Override
  public boolean isEnabled(Feature key) {
    return isEnabled(key.name());
  }
}
