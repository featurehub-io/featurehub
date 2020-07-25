package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;

import java.util.concurrent.Executor;

public class FeatureStatePlaceHolder extends FeatureStateBaseHolder {
  public FeatureStatePlaceHolder(Executor executor) {
    super(executor);
  }

  @Override
  protected FeatureStateHolder setFeatureState(FeatureState state) {
    return this;
  }

  @Override
  protected FeatureStateHolder copy() {
    return this;
  }
}
