package io.featurehub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.sse.model.FeatureState;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

class FeatureStateJsonHolder extends FeatureStateBaseHolder {
  private final ObjectMapper mapper;
  private String value;

  public FeatureStateJsonHolder(FeatureStateBaseHolder holder, Executor executor, ObjectMapper mapper,
                                List<FeatureValueInterceptor> valueInterceptors, String key) {
    super(executor, holder, valueInterceptors, key);
    this.mapper = mapper;
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
  protected FeatureStateHolder copy() {
    return new FeatureStateJsonHolder(null, null, mapper, valueInterceptors, key).setFeatureState(featureState);
  }

  @Override
  public String getRawJson() {
    FeatureValueInterceptor.ValueMatch vm = findIntercept();

    if (vm != null) {
      return vm.value;
    }

    return value;
  }

  @Override
  public <T> T getJson(Class<T> type) {
    try {
      return value == null ? null : mapper.readValue(value, type);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isSet() {
    return getRawJson() != null;
  }
}
