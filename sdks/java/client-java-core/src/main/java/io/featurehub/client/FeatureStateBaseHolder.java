package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.FeatureValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is just the base class to avoid a whole lot of duplication effort and to ensure the
 * maximum performance for each feature in updating its listeners and knowing what type it is.
 */
public class FeatureStateBaseHolder implements FeatureStateHolder {
  private static final Logger log = LoggerFactory.getLogger(FeatureStateBaseHolder.class);
  protected final String key;
  protected FeatureState featureState;
  List<FeatureListener> listeners = new ArrayList<>();
  protected ClientContext context;
  protected FeatureStore featureStore;
  protected Object value;

  public FeatureStateBaseHolder(
      FeatureStateBaseHolder oldHolder, FeatureStore featureStore, String key) {
    this(featureStore, key);

    if (oldHolder != null) {
      this.listeners = oldHolder.listeners;
    }
  }

  public FeatureStateBaseHolder(FeatureStore featureStore, String key) {
    this.key = key;
    this.featureStore = featureStore;
  }

  public FeatureStateHolder withContext(ClientContext context) {
    return ((FeatureStateBaseHolder) copy()).setContext(context);
  }

  protected FeatureStateHolder setContext(ClientContext ctx) {
    this.context = ctx;
    return this;
  }

  protected void notifyListeners() {
    listeners.forEach(
        (sl) -> featureStore.execute(() -> sl.notify(this)));
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getString() {
    return getAsString(FeatureValueType.STRING);
  }

  private String getAsString(FeatureValueType type) {
    FeatureValueInterceptor.ValueMatch vm = findIntercept();

    if (vm != null) {
      return vm.value;
    }

    if (featureState == null || featureState.getType() != type) {
      return null;
    }

    if (context != null) {
      final Applied applied =
        featureStore.applyFeature(
          featureState.getStrategies(), key, featureState.getId(), context);

      if (applied.isMatched()) {
        return applied.getValue() == null ? null : applied.getValue().toString();
      }
    }

    return value == null ? null : value.toString();
  }

  @Override
  public Boolean getBoolean() {
    FeatureValueInterceptor.ValueMatch vm = findIntercept();

    if (vm != null) {
      return Boolean.parseBoolean(vm.value);
    }

    if (featureState == null || featureState.getType() != FeatureValueType.BOOLEAN) {
      return null;
    }

    if (context != null) {
      final Applied applied =
          featureStore.applyFeature(
              featureState.getStrategies(), key, featureState.getId(), context);

      if (applied.isMatched()) {
        return Boolean.TRUE.equals(applied.getValue());
      }
    }

    return Boolean.TRUE.equals(value);
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

    if (featureState == null || featureState.getType() != FeatureValueType.NUMBER) {
      return null;
    }

    if (context != null) {
      final Applied applied =
        featureStore.applyFeature(
          featureState.getStrategies(), key, featureState.getId(), context);

      if (applied.isMatched()) {
        return applied.getValue() == null ? null : new BigDecimal(applied.getValue().toString());
      }
    }

    return (BigDecimal)value;
  }

  @Override
  public String getRawJson() {
    return getAsString(FeatureValueType.JSON);
  }

  @Override
  public <T> T getJson(Class<T> type) {
    String rawJson = getRawJson();

    try {
      return rawJson == null ? null : featureStore.getJsonObjectMapper().readValue(rawJson, type);
    } catch (IOException e) {
      log.warn("Failed to parse JSON", e);
      return null;
    }
  }

  @Override
  public boolean isSet() {
    return value != null;
  }

  protected FeatureValueInterceptor.ValueMatch findIntercept() {
    boolean locked = featureState != null && Boolean.TRUE.equals(featureState.getL());
    return featureStore.getFeatureValueInterceptors().stream()
        .filter(vi -> !locked || vi.allowLockOverride)
        .map(
            vi -> {
              FeatureValueInterceptor.ValueMatch vm = vi.interceptor.getValue(key);
              if (vm != null && vm.matched) {
                return vm;
              } else {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  @Override
  public void addListener(FeatureListener listener) {
    listeners.add(listener);
  }

  public FeatureStateHolder setFeatureState(FeatureState featureState) {
    if (featureState != null) {
      this.featureState = featureState;

      Object oldValue = value;

      if (featureState.getValue() == null) {
        value = null;
      } else {
        try {
          switch (featureState.getType()) {
            case BOOLEAN:
              value = Boolean.parseBoolean(featureState.getValue().toString());
              break;
            case STRING:
              value = featureState.getValue().toString();
              break;
            case NUMBER:
              value = new BigDecimal(featureState.getValue().toString());
              break;
            case JSON:
              value = featureState.getValue().toString();
              break;
          }
        } catch (Exception e) {
          value = null;
        }
      }

      if (FeatureStateUtils.changed(oldValue, value)) {
        notifyListeners();
      }
    }

    return this;
  }

  protected FeatureStateHolder copy() {
    return new FeatureStateBaseHolder(this, featureStore, key).setFeatureState(featureState);
  }

  protected boolean exists() {
    return featureState != null;
  }

  protected FeatureValueType type() {
    return featureState == null ? null : featureState.getType();
  }

  @Override
  public String toString() {
    return value == null ? null : value.toString();
  }
}
