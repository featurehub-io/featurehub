package io.featurehub.client;

import java.util.Map;

abstract public class AbstractFeatureRepository implements FeatureRepository {

  @Override
  public FeatureState getFeatureState(Feature feature) {
    return this.getFeatureState(feature.name());
  }

  @Override
  public boolean exists(Feature key) {
    return exists(key.name());
  }

  @Override
  public boolean isEnabled(Feature key) {
    return isEnabled(key.name());
  }

  @Override
  public boolean isEnabled(String name) {
    return Boolean.TRUE.equals(getFeatureState(name).getBoolean());
  }

  @Override
  public FeatureRepository logAnalyticsEvent(String action, Map<String, String> other) {
    return logAnalyticsEvent(action, other, null);
  }

  @Override
  public FeatureRepository logAnalyticsEvent(String action, ClientContext ctx) {
    return logAnalyticsEvent(action, null, ctx);
  }

  @Override
  public FeatureRepository logAnalyticsEvent(String action) {
    return logAnalyticsEvent(action, null, null);
  }
}
