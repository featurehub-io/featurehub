package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;

public interface FeatureStateHolderInternal extends FeatureStateHolder {
  void setFeatureState(FeatureState state);
}
