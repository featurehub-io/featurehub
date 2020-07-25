package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

public class FeatureStateNumberHolder extends FeatureStateBaseHolder {
  private BigDecimal value;

  public FeatureStateNumberHolder(FeatureStateBaseHolder holder, Executor executor) {
    super(executor, holder);
  }

  public FeatureStateHolder setFeatureState(FeatureState featureState) {
    this.featureState = featureState;
    BigDecimal oldValue = value;
    value = featureState.getValue() == null ? null : new BigDecimal(featureState.getValue().toString());
    if (FeatureStateUtils.changed(oldValue, value)) {
      notifyListeners();
    }
    return this;
  }

  @Override
  protected FeatureStateHolder copy() {
    return new FeatureStateNumberHolder(null, null).setFeatureState(featureState);
  }

  @Override
  public BigDecimal getNumber() {
    String dev = devOverride();

    if (dev != null) {
      return new BigDecimal(dev);
    }

    return value;
  }

  @Override
  public boolean isSet() {
    return getNumber() != null;
  }
}
