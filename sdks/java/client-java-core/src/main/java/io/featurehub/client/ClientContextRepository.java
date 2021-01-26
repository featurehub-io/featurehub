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

public class ClientContextRepository implements ClientContext {
  private static final Logger log = LoggerFactory.getLogger(ClientContextRepository.class);
  private final Map<String, List<String>> clientContext = new ConcurrentHashMap<>();
  private final List<ClientContextChanged> listeners = new ArrayList<>();
  private final Executor executor;

  public ClientContextRepository(Executor executor) {
    this.executor = executor;
  }

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
  public ClientContext build() {
    executor.execute(() -> {
      String header = generateHeader();

      for(ClientContextChanged l : listeners) {
        try {
          l.notify(header);
        } catch (Exception e) {
          log.error("Failed to set header: {}", header, e);
        }
      }
    });
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

  private String generateHeader() {
    if (clientContext.isEmpty()) {
      return null;
    }

    return clientContext.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(),
     URLEncoder.encode(String.join(",", e.getValue())))).sorted().collect(Collectors.joining(","));
  }

  @Override
  public ClientContextListenerRemoval registerChangeListener(ClientContextChanged listener) {
    // immediately trigger it
    executor.execute(() -> {
      try {
        listener.notify(generateHeader());
        listeners.add(listener);
      } catch (Exception e) {
        log.error("Unable to trigger listener", e);
      }
    });

    return () -> {
      listeners.remove(listener);
    };
  }
}
