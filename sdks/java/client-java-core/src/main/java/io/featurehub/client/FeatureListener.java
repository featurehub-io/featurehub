package io.featurehub.client;

public interface FeatureListener {
  void notify(FeatureState featureChanged);
}
