package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

public class FeatureStateNumberHolder extends FeatureStateBaseHolder {
  private static final Logger log = LoggerFactory.getLogger(FeatureStateNumberHolder.class);
  private BigDecimal value;

  public FeatureStateNumberHolder(FeatureStateBaseHolder holder, Executor executor,
                                  List<FeatureValueInterceptorHolder> valueInterceptors, String key) {
    super(executor, holder, valueInterceptors, key);
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
    return new FeatureStateNumberHolder(null, null, valueInterceptors, key).setFeatureState(featureState);
  }

  @Override
  public BigDecimal getNumber() {
    FeatureValueInterceptor.ValueMatch vm = findIntercept();

    if (vm != null) {
      try {
        return (vm.value == null) ? null : new BigDecimal(vm.value);
      } catch (Exception e) {
        log.warn("Attempting to convert {} to BigDecimal fails as is not a number", vm.value);
        return null; // ignore conversion failures
      }
    }

    return value;
  }

  @Override
  public boolean isSet() {
    return getNumber() != null;
  }
}
