package io.featurehub.client;

abstract public class AbstractFeatureRepository implements FeatureRepository {

  @Override
  public FeatureState getFeatureState(Feature feature) {
    return this.getFeatureState(feature.name());
  }
}
