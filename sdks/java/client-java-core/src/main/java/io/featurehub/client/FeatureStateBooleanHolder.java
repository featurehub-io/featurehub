package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;

import java.util.List;
import java.util.concurrent.Executor;

public class FeatureStateBooleanHolder extends FeatureStateBaseHolder {
  private Boolean value;

  public FeatureStateBooleanHolder(FeatureStateBaseHolder holder, Executor executor,
                                   List<FeatureValueInterceptorHolder> valueInterceptors, String key) {
    super(executor, holder, valueInterceptors, key);
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
    return new FeatureStateBooleanHolder(null, null, valueInterceptors, key).setFeatureState(featureState);
  }

  @Override
  public Boolean getBoolean() {
    FeatureValueInterceptor.ValueMatch vm = findIntercept();

    if (vm != null) {
      return Boolean.parseBoolean(vm.value);
    }

    return value;
  }

  @Override
  public boolean isSet() {
    return getBoolean() != null;
  }
}
