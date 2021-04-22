package io.featurehub.client;

import io.featurehub.sse.model.StrategyAttributeCountryName;
import io.featurehub.sse.model.StrategyAttributeDeviceName;
import io.featurehub.sse.model.StrategyAttributePlatformName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface ClientContext {
  String get(String key, String defaultValue);

  ClientContext userKey(String userKey);
  ClientContext sessionKey(String sessionKey);
  ClientContext country(StrategyAttributeCountryName countryName);
  ClientContext device(StrategyAttributeDeviceName deviceName);
  ClientContext platform(StrategyAttributePlatformName platformName);
  ClientContext version(String version);
  ClientContext attr(String name, String value);
  ClientContext attrs(String name, List<String> values);

  ClientContext clear();

  /**
   * Triggers the build and setting of this context.
   *
   * @return this
   */
  Future<ClientContext> build();

  Map<String, List<String>> context();
  String defaultPercentageKey();

  FeatureState feature(String name);
  FeatureState feature(Feature name);

  FeatureRepository getRepository();
  EdgeService getEdgeService();

  ClientContext logAnalyticsEvent(String action, Map<String, String> other);
  ClientContext logAnalyticsEvent(String action);

  /**
   * true if it is a boolean feature and is true within this context.
   *
   * @param name
   * @return false if not true or not boolean, true otherwise.
   */
  boolean isEnabled(String name);
  boolean isEnabled(Feature name);

  boolean isSet(String name);
  boolean isSet(Feature name);

  boolean exists(String key);
  boolean exists(Feature key);

  void close();
}
