package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;

import java.util.concurrent.Executor;

public class FeatureStateBooleanHolder extends FeatureStateBaseHolder {
  private Boolean value;

  public FeatureStateBooleanHolder(FeatureStateBaseHolder holder, Executor executor) {
    super(executor, holder);
  }

  public FeatureStateHolder setFeatureState(FeatureState featureState) {
    this.featureState = featureState;
    Boolean oldValue = value;
    value = featureState.getValue() == null ? null : Boolean.TRUE.equals(featureState.getValue());
    if (FeatureStateUtils.changed(oldValue, value)) {
      notifyListeners();
    }
    return this;
  }

  @Override
  protected FeatureStateHolder copy() {
    return new FeatureStateBooleanHolder(null, null).setFeatureState(featureState);
  }

  @Override
  public Boolean getBoolean() {
    String dev = devOverride();

    if (dev != null) {
      return Boolean.parseBoolean(dev);
    }

    return value;
  }

  @Override
  public boolean isSet() {
    return getBoolean() != null;
  }
}
