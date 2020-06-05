package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

class FeatureStateStringHolder extends FeatureStateBaseHolder {
  private static final Logger log = LoggerFactory.getLogger(FeatureStateStringHolder.class);
  private String value;

  public FeatureStateStringHolder(FeatureStateBaseHolder holder, Executor executor) {
    super(executor, holder);
  }

  public FeatureStateHolder setFeatureState(FeatureState featureState) {
    this.featureState = featureState;
    String oldValue = value;
    value = featureState.getValue() == null ? null : featureState.getValue().toString();
    if (FeatureStateUtils.changed(oldValue, value)) {
      notifyListeners();
    }
    return this;
  }

  @Override
  public boolean isSet() {
    return getString() != null;
  }

  @Override
  protected FeatureStateHolder copy() {
    return new FeatureStateStringHolder(null, null).setFeatureState(featureState);
  }

  @Override
  public String getString() {
    String dev = devOverride();

    if (dev != null) {
      return dev;
    }

    return value;
  }
}
