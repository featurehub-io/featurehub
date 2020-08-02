package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;


/**
 * This class is just the base class to avoid a whole lot of duplication effort and
 * to ensure the maximum performance for each feature in updating its listeners and knowing
 * what type it is.
 */
abstract class FeatureStateBaseHolder implements FeatureStateHolder {
  protected final List<FeatureValueInterceptor> valueInterceptors;
  protected final String key;
  private final Executor executor;
  protected FeatureState featureState;
  List<FeatureListener> listeners = new ArrayList<>();

  public FeatureStateBaseHolder(Executor executor, FeatureStateBaseHolder oldHolder,
                                List<FeatureValueInterceptor> valueInterceptors, String key) {
    this(executor, valueInterceptors, key);

    if (oldHolder != null) {
      this.listeners = oldHolder.listeners;
    }
  }

  public FeatureStateBaseHolder(Executor executor, List<FeatureValueInterceptor> valueInterceptors, String key) {
    this.executor = executor;
    this.valueInterceptors = valueInterceptors;
    this.key = key;
  }

  protected void notifyListeners() {
    listeners.forEach((sl) -> {
      executor.execute(() -> sl.notify(this));
    });
  }

  @Override
  public String getKey() {
    return featureState == null ? null : featureState.getKey();
  }

  @Override
  public String getString() {
    return null;
  }

  @Override
  public Boolean getBoolean() {
    return null;
  }

  @Override
  public BigDecimal getNumber() {
    return null;
  }

  @Override
  public String getRawJson() {
    return null;
  }

  @Override
  public <T> T getJson(Class<T> type) {
    return null;
  }

  @Override
  public boolean isSet() {
    return false;
  }

  protected FeatureValueInterceptor.ValueMatch findIntercept() {
    return valueInterceptors.stream().map(vi -> {
      FeatureValueInterceptor.ValueMatch vm = vi.getValue(key);
      if (vm.matched) {
        return vm;
      } else {
        return null;
      }
    }).filter(Objects::nonNull).findFirst().orElse(null);
  }

  // wait for integration with OpenTelemetry/OpenTracing
//    if (System.getProperty(FEATURE_TOGGLES_ALLOW_OVERRIDE) != null ) {
//      String override = Optional.ofNullable(GlobalTracer.get())
//        .map(Tracer::activeSpan)
//        .filter(Objects::nonNull)
//        .map(span -> span.getBaggageItem(ACCELERATE_FEATURE_OVERRIDE)).orElse(null);

//      if (override != null) {
//        if (override.contains(String.format("%s=true", feature.key()))) {
//          return true;
//        }
//        if (override.contains(String.format("%s=false", feature.key()))) {
//          return false;
//        }
//      }
//    }

  @Override
  public void addListener(FeatureListener listener) {
    listeners.add(listener);
  }

  protected abstract FeatureStateHolder setFeatureState(FeatureState state);

  protected abstract FeatureStateHolder copy();
}
