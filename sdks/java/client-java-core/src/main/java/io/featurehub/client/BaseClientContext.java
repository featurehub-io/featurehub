package io.featurehub.client;

import io.featurehub.sse.model.StrategyAttributeCountryName;
import io.featurehub.sse.model.StrategyAttributeDeviceName;
import io.featurehub.sse.model.StrategyAttributePlatformName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public abstract class BaseClientContext implements ClientContext {
  private static final Logger log = LoggerFactory.getLogger(BaseClientContext.class);
  private final Map<String, List<String>> clientContext = new ConcurrentHashMap<>();

  @Override
  public String get(String key, String defaultValue) {
    if (clientContext.containsKey(key)) {
      final List<String> vals = clientContext.get(key);
      return vals.isEmpty() ? defaultValue : vals.get(0);
    }

    return defaultValue;
  }

  @Override
  public ClientContext userKey(String userKey) {
    clientContext.put("userkey", Collections.singletonList(userKey));
    return this;
  }

  @Override
  public ClientContext sessionKey(String sessionKey) {
    clientContext.put("session", Collections.singletonList(sessionKey));
    return this;
  }

  @Override
  public ClientContext country(StrategyAttributeCountryName countryName) {
    clientContext.put("country", Collections.singletonList(countryName.toString()));
    return this;
  }

  @Override
  public ClientContext device(StrategyAttributeDeviceName deviceName) {
    clientContext.put("device", Collections.singletonList(deviceName.toString()));
    return this;
  }

  @Override
  public ClientContext platform(StrategyAttributePlatformName platformName) {
    clientContext.put("platform", Collections.singletonList(platformName.toString()));
    return this;
  }

  @Override
  public ClientContext version(String version) {
    clientContext.put("version", Collections.singletonList(version));
    return this;
  }

  @Override
  public ClientContext attr(String name, String value) {
    clientContext.put(name, Collections.singletonList(value));
    return this;
  }

  @Override
  public ClientContext attrs(String name, List<String> values) {
    clientContext.put(name, values);
    return this;
  }

  @Override
  public ClientContext clear() {
    clientContext.clear();
    return this;
  }

  @Override
  public Map<String, List<String>> context() {
    return clientContext;
  }

  @Override
  public String defaultPercentageKey() {
    if (clientContext.containsKey("session")) {
      return clientContext.get("session").get(0);
    } else if (clientContext.containsKey("userkey")) {
      return clientContext.get("userkey").get(0);
    }

    return null;
  }

  @Override
  public FeatureState feature(String name) {
    final FeatureState fs = getRepository().getFeatureState(name);

    return getRepository().isServerEvaluation() ? fs : fs.withContext(this);
  }

  @Override
  public FeatureState feature(Feature name) {
    return feature(name.name());
  }

  @Override
  public boolean isEnabled(Feature name) {
    return isEnabled(name.name());
  }

  @Override
  public boolean isEnabled(String name) {
    // we use this mechanism as it will return the state within the context (vs repository which might be different)
    return feature(name).getBoolean() == Boolean.TRUE;
  }

  @Override
  public boolean exists(Feature key) {
    return exists(key.name());
  }
}
