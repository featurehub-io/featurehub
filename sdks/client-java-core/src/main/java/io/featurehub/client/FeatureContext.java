package io.featurehub.client;

import java.util.Map;

public interface FeatureContext {
  /**
   * Determine if a feature flag is set (or overridden either by local settings or
   * OpenTelemetry).
   *
   * @param feature - the feature we are using
   * @return - true if the feature is active. Does not determine if the feature even exists in the back-end system
   */
  boolean isActive(Feature feature);

  /**
   * Determines if the feature exists in the back-end system. This will always ask the repository and cannot be
   * overridden by local settings or open telemetry.
   *
   * @param feature - the feature we are checking to see if exists
   * @return - true if the feature exists. If we haven't received a response yet from the back-end due to communication
   * issues this will return false.
   */
  boolean exists(Feature feature);

  /**
   * determine if the value of the feature is actually not set. For flags this can be the difference between true
   * and false, as true is only true if the flag is true, it is false if false or not-set.
   *
   * @param feature - the feature we are checking for
   * @return - true if the value is not null
   */
  boolean isSet(Feature feature);

  /**
   * Adds a listener that is triggered whenever the state of the specified feature or configuration changes.
   *
   * @param feature - feature name
   * @param featureListener - a notify listener that returns the FeatureStateHolder
   */
  void addListener(Feature feature, FeatureListener featureListener);

  /**
   * Logs an analytics event with any (and all) of the analytics providers. Currently this is done directly from the
   * instance this is running on. The state of all features are passed.
   *
   * @param action - passed directly as a name or key
   * @param other - any other daya (or null)
   */
  void logAnalyticsEvent(String action, Map<String, String> other);

  /**
   * Logs an analytics event with any (and all) of the analytics providers. Currently this is done directly from the
   * instance this is running on. The state of all features are passed. Convenience method.
   *
   * @param action - passed directly as a name or key
   */
  void logAnalyticsEvent(String action);
}
