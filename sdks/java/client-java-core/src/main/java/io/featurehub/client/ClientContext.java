package io.featurehub.client;

import io.featurehub.sse.model.StrategyAttributeCountryName;
import io.featurehub.sse.model.StrategyAttributeDeviceName;
import io.featurehub.sse.model.StrategyAttributePlatformName;

import java.util.List;
import java.util.Map;

public interface ClientContext {
  String get(String key, String defaultValue);

  interface ClientContextChanged {
    /**
     *
     * @param header a correctly formatted header that has been url escaped or null (if no properties)
     */
    void notify(String header);
  }

  interface  ClientContextListenerRemoval {
    void remove();
  }

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
  ClientContext build();

  Map<String, List<String>> context();
  String defaultPercentageKey();

  ClientContextListenerRemoval registerChangeListener(ClientContextChanged listener);
}
