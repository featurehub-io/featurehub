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
  protected final List<FeatureValueInterceptorHolder> valueInterceptors;
  protected final String key;
  private final Executor executor;
  protected FeatureState featureState;
  List<FeatureListener> listeners = new ArrayList<>();
  protected ClientContext context;

  public FeatureStateBaseHolder(Executor executor, FeatureStateBaseHolder oldHolder,
                                List<FeatureValueInterceptorHolder> valueInterceptors, String key) {
    this(executor, valueInterceptors, key);

    if (oldHolder != null) {
      this.listeners = oldHolder.listeners;
    }
  }

  public FeatureStateBaseHolder(Executor executor, List<FeatureValueInterceptorHolder> valueInterceptors, String key) {
    this.executor = executor;
    this.valueInterceptors = valueInterceptors;
    this.key = key;
  }

  public FeatureStateHolder withContext(ClientContext context) {
    return ((FeatureStateBaseHolder)copy()).setContext(context);
  }

  protected FeatureStateHolder setContext(ClientContext ctx) {
    this.context = ctx;
    return this;
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
    boolean locked = featureState != null && Boolean.TRUE.equals(featureState.getL());
    return valueInterceptors.stream()
      .filter(vi -> !locked || vi.allowLockOverride )
      .map(vi -> {
      FeatureValueInterceptor.ValueMatch vm = vi.interceptor.getValue(key);
      if (vm != null && vm.matched) {
        return vm;
      } else {
        return null;
      }
    }).filter(Objects::nonNull).findFirst().orElse(null);
  }

  @Override
  public void addListener(FeatureListener listener) {
    listeners.add(listener);
  }

  protected abstract FeatureStateHolder setFeatureState(FeatureState state);

  protected abstract FeatureStateHolder copy();
}
