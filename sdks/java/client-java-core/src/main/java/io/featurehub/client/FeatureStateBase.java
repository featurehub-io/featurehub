package io.featurehub.client;

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
public class FeatureStateBase implements FeatureState {
  private static final Logger log = LoggerFactory.getLogger(FeatureStateBase.class);
  protected final String key;
  protected io.featurehub.sse.model.FeatureState _featureState;
  List<FeatureListener> listeners = new ArrayList<>();
  protected ClientContext context;
  protected FeatureStore featureStore;
  protected FeatureStateBase parentHolder;

  public FeatureStateBase(
    FeatureStateBase oldHolder, FeatureStore featureStore, String key) {
    this(featureStore, key);

    if (oldHolder != null) {
      this.listeners = oldHolder.listeners;
    }
  }

  public FeatureStateBase(FeatureStore featureStore, String key) {
    this.key = key;
    this.featureStore = featureStore;
  }

  public FeatureState withContext(ClientContext context) {
    final FeatureStateBase copy = _copy();
    copy.context = context;
    return copy;
  }

  protected io.featurehub.sse.model.FeatureState featureState() {
    // clones for analytics will set the feature state
    if (_featureState != null) {
      return _featureState;
    }

    // child objects for contexts will use this
    if (parentHolder != null) {
      return parentHolder.featureState();
    }

    // otherwise it isn't set
    return null;
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
  public boolean isLocked() {
    return this.featureState() != null && this.featureState().getL() == Boolean.TRUE;
  }

  @Override
  public String getString() {
    return getAsString(FeatureValueType.STRING);
  }

  @Override
  public Boolean getBoolean() {
    Object val = getValue(FeatureValueType.BOOLEAN);

    if (val == null) {
      return null;
    }

    if (val instanceof String) {
      return Boolean.TRUE.equals("true".equalsIgnoreCase(val.toString()));
    }

    return Boolean.TRUE.equals(val);
  }

  private Object getValue(FeatureValueType type) {
    // unlike js, locking is registered on a per interceptor basis
    FeatureValueInterceptor.ValueMatch vm = findIntercept();

    if (vm != null) {
      return vm.value;
    }

    final io.featurehub.sse.model.FeatureState featureState = featureState();
    if (featureState == null || featureState.getType() != type) {
      return null;
    }

    if (context != null) {
      final Applied applied =
        featureStore.applyFeature(
          featureState.getStrategies(), key, featureState.getId(), context);

      if (applied.isMatched()) {
        return applied.getValue() == null ? null : applied.getValue();
      }
    }

    return featureState.getValue();
  }

  private String getAsString(FeatureValueType type) {
    Object value = getValue(type);
    return value == null ? null : value.toString();
  }

  @Override
  public BigDecimal getNumber() {
    Object val = getValue(FeatureValueType.NUMBER);

    try {
      return (val == null) ? null : (val instanceof BigDecimal ? ((BigDecimal)val) : new BigDecimal(val.toString()));
    } catch (Exception e) {
      log.warn("Attempting to convert {} to BigDecimal fails as is not a number", val);
      return null; // ignore conversion failures
    }
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
  public boolean isEnabled() {
    return getBoolean() == Boolean.TRUE;
  }

  @Override
  public boolean isSet() {
    return featureState() != null && getAsString(featureState().getType()) != null;
  }

  protected FeatureValueInterceptor.ValueMatch findIntercept() {
    boolean locked = featureState() != null && Boolean.TRUE.equals(featureState().getL());
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
  public void addListener(final FeatureListener listener) {
    if (context != null) {
      listeners.add((fs) -> listener.notify(this));
    } else {
      listeners.add(listener);
    }
  }

  // stores the feature state and triggers notifyListeners if anything changed
  // should the notify actually be inside the listener code? given contexts?
  public FeatureState setFeatureState(io.featurehub.sse.model.FeatureState featureState) {
    if (featureState != null) {
      Object oldValue = getValue(type());

      this._featureState = featureState;

      Object value = null;

      if (featureState.getValue() != null) {
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
        } catch (Exception ignored) {
        }
      }

      if (FeatureStateUtils.changed(oldValue, value)) {
        notifyListeners();
      }
    }

    return this;
  }

  protected FeatureState copy() {
    return _copy();
  }

  protected FeatureState analyticsCopy() {
    final FeatureStateBase aCopy = _copy();
    aCopy._featureState = featureState();
    return aCopy;
  }

  protected FeatureStateBase _copy() {
    final FeatureStateBase copy = new FeatureStateBase(this, featureStore, key);
    copy.parentHolder = this;
    return copy;
  }

  protected boolean exists() {
    return featureState() != null;
  }

  protected FeatureValueType type() {
    final io.featurehub.sse.model.FeatureState featureState = featureState();
    return featureState == null ? null : featureState.getType();
  }

  @Override
  public String toString() {
    Object value = getValue(type());
    return value == null ? null : value.toString();
  }
}
