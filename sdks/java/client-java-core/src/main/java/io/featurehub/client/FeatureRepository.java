package io.featurehub.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;

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
   * Update the feature states and force them to be updated, ignoring their versin numbers.
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
   */
  FeatureRepository addReadynessListener(ReadynessListener readynessListener);

  /**
   * Get a feature state isolated from the API.
   *
   * @param key
   * @return the FeatureStateHolder referring to this key, can exist but not refer to an actual feature
   */

  FeatureStateHolder getFeatureState(String key);

  /**
   * Log an analytics event against the analytics collectors.
   *
   * @param action
   * @param other
   */
  FeatureRepository logAnalyticsEvent(String action, Map<String, String> other);

  /**
   * Register an analytics collector
   *
   * @param collector - a class implementing the AnalyticsCollector interface
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
