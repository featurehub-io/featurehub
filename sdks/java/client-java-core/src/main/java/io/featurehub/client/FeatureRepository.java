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
   * @deprecated
   * Get a feature state isolated from the API. Always try and use the context.
   *
   * @param key - the key of the feature
   * @return - the FeatureStateHolder referring to this key, can exist but not refer to an actual feature
   */
  FeatureState getFeatureState(String key);
  FeatureState getFeatureState(Feature feature);

  // replaces getFlag and its myriad combinations with a pure boolean response, true if set and is true, otherwise false

  /**
   * @deprecated - please migrate to using the ClientContext
   */
  boolean isEnabled(String name);
  /**
   * @deprecated - please migrate to using the ClientContext
   */
  boolean isEnabled(Feature key);

  /**
   * @deprecated - please migrate to using the ClientContext
   */
  FeatureRepository logAnalyticsEvent(String action, Map<String, String> other);
  /**
   * @deprecated - please migrate to using the ClientContext
   */
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

  /**
   * @deprecated - please migrate to using the ClientContext
   */
  boolean exists(String key);
  /**
   * @deprecated - please migrate to using the ClientContext
   */
  boolean exists(Feature key);

  boolean isServerEvaluation();

  void close();
}
