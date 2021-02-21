package io.featurehub.client;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

public interface FeatureRepository {
  /**
   * Changes in readyness for the repository. It can become ready and then fail if subsequent
   * calls fail.
   *
   * @param readynessListener - a callback lambda
   * @return - this FeatureRepository
   */
  FeatureRepository addReadynessListener(ReadynessListener readynessListener);

  /**
   * Get a feature state isolated from the API.
   *
   * @param key - the key of the feature
   * @return - the FeatureStateHolder referring to this key, can exist but not refer to an actual feature
   */
  FeatureState getFeatureState(String key);
  FeatureState getFeatureState(Feature feature);

  // replaces getFlag and its myriad combinations with a pure boolean response, true if set and is true, otherwise false
  boolean isEnabled(String name);
  boolean isEnabled(Feature key);

  /**
   * Log an analytics event against the analytics collectors.
   *
   * @param action - the action you wish to log with your analytics provider
   * @param other - any other data
   * @return - this
   */
  FeatureRepository logAnalyticsEvent(String action, Map<String, String> other);
  FeatureRepository logAnalyticsEvent(String action);
  FeatureRepository logAnalyticsEvent(String action, Map<String, String> other, ClientContext ctx);
  FeatureRepository logAnalyticsEvent(String action, ClientContext ctx);

  /**
   * Register an analytics collector
   *
   * @param collector - a class implementing the AnalyticsCollector interface
   * @return - thimvn s
   */
  FeatureRepository addAnalyticCollector(AnalyticsCollector collector);

  /**
   * Adds interceptor support for feature values.
   *
   * @param allowLockOverride - is this interceptor allowed to override the lock? i.e. if the feature is locked, we
   *                          ignore the interceptor
   * @param interceptor - the interceptor
   * @return the instance of the repo for chaining
   */
  FeatureRepository registerValueInterceptor(boolean allowLockOverride, FeatureValueInterceptor interceptor);

  /**
   * Is this repository ready to connect to.
   *
   * @return Readyness status
   */
  Readyness getReadyness();

  /**
   * Lets the SDK override the configuration of the JSON mapper in case they have special techniques they use.
   *
   * @param jsonConfigObjectMapper - an ObjectMapper configured for client use. This defaults to the same one
   *                               used to deserialize
   */
  void setJsonConfigObjectMapper(ObjectMapper jsonConfigObjectMapper);

  boolean exists(String key);
  boolean exists(Feature key);

  boolean isServerEvaluation();


  void close();
}
