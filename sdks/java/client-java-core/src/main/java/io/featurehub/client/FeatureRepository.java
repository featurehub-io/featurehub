package io.featurehub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FeatureRepository {

  /*
   * Any incoming state changes from a multi-varied set of possible data. This comes
   * from SSE.
   */
  void notify(SSEResultState state, String data);

  /**
   * Indicate the feature states have updated and if their versions have
   * updated or no versions exist, update the repository.
   *
   * @param states - the features
   */
  void notify(List<FeatureState> states);


  /**
   * Update the feature states and force them to be updated, ignoring their version numbers.
   * This still may not cause events to be triggered as event triggers are done on actual value changes.
   *
   * @param states - the list of feature states
   * @param force  - whether we should force the states to change
   */
  void notify(List<FeatureState> states, boolean force);

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
  FeatureStateHolder getFeatureState(String key);
  FeatureStateHolder getFeatureState(Feature feature);

  /**
   * The value of the flag/boolean feature.
   *
   * @param key - the feature key
   * @return - true or false depending on the flag. If the feature doesn't exist, it will return false.
   */
  boolean getFlag(String key);
  boolean getFlag(Feature feature);

  /**
   * The value of the string feature.
   *
   * @param key - the feature key
   * @return - the value of the string feature or null if it is unset or doesn't exist.
   */
  String getString(String key);
  String getString(Feature feature);



  /**
   * The value of the number feature.
   *
   * @param key - the feature key
   * @return - the value of the number feature or null if it is unset or doesn't exist.
   */
  BigDecimal getNumber(String key);
  BigDecimal getNumber(Feature feature);

  /**
   * The value of the json feature decoded into the correct class (if possible).
   *
   * @param key - the feature key
   * @param type - the class type - as an Class.class
   * @param <T> - the type of the class you want back
   * @return - the value of the json feature or null if it is unset or doesn't exist. If it cannot be decoded then it
   * may throw an exception.
   */
  <T> T getJson(String key, Class<T> type);
  <T> T getJson(Feature feature, Class<T> type);

  /**
   * The value of the json feature in string form.
   *
   * @param key - the feature key
   * @return - the value of the json feature or null if it is unset or doesn't exist.
   */
  String getRawJson(String key);
  String getRawJson(Feature feature);

  /**
   * Returns whether there is a value associated with this feature. Boolean features only return false if there
   * is in fact no feature.
   *
   * @param key - the feature key
   * @return - true or false
   */
  boolean isSet(String key);
  boolean isSet(Feature feature);

  /**
   * Returns whether this feature does in fact not exist.
   *
   * @param key - the feature key
   * @return - true or false
   */
  boolean exists(String key);
  boolean exists(Feature feature);

  /**
   * Log an analytics event against the analytics collectors.
   *
   * @param action - the action you wish to log with your analytics provider
   * @param other - any other data
   * @return - this
   */
  FeatureRepository logAnalyticsEvent(String action, Map<String, String> other);

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

  ClientContext clientContext();
}
