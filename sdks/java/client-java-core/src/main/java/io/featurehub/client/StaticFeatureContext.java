package io.featurehub.client;

import java.util.Map;

/**
 * this is the only one independent of system. in Spring or JAX-RS this can be wired into the system
 * and injected.
 */
public class StaticFeatureContext implements FeatureContext {
  public static FeatureRepository repository;

  private static StaticFeatureContext self;

  public static StaticFeatureContext getInstance() {
    if (self == null) {
      self = new StaticFeatureContext();
    }
    return self;
  }

  @Override
  public boolean isActive(Feature feature) {
    if (repository == null) {
      throw new RuntimeException("Repository not set.");
    }

    return FeatureStateUtils.isActive(repository, feature);
  }

  @Override
  public boolean exists(Feature feature) {
    if (repository == null) {
      throw new RuntimeException("Repository not set.");
    }

    return FeatureStateUtils.exists(repository, feature);
  }

  @Override
  public boolean isSet(Feature feature) {
    if (repository == null) {
      throw new RuntimeException("Repository not set.");
    }

    return FeatureStateUtils.isSet(repository, feature);
  }

  @Override
  public void addListener(Feature feature, FeatureListener featureListener) {
    if (repository == null) {
      throw new RuntimeException("Repository not set.");
    }

    FeatureStateUtils.addListener(repository, feature, featureListener);
  }

  @Override
  public void logAnalyticsEvent(String action, Map<String, String> other) {
    if (repository == null) {
      throw new RuntimeException("Repository not set.");
    }

    repository.logAnalyticsEvent(action, other);
  }

  @Override
  public void logAnalyticsEvent(String action) {
    logAnalyticsEvent(action, null);
  }
}
