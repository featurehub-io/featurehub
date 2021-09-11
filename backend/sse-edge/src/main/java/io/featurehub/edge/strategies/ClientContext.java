package io.featurehub.edge.strategies;

import io.featurehub.edge.KeyParts;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The client attributes are expected to be in the format key=val where val is url-encoded. It can
 * have multiple values separated by commas and there can be multiple headers.
 */
public class ClientContext {
  public Map<String, List<String>> attributes = new HashMap<>();
  public static final String USERKEY = "userkey";
  public static final String SESSIONKEY = "sessionkey";
  public final boolean isClientEvaluation;

  public ClientContext(boolean isClientEvaluation) {
    this.isClientEvaluation = isClientEvaluation;
  }

  String defaultPercentageKey() {
    List<String> uKey = attributes.get(SESSIONKEY);

    if (uKey == null) {
      uKey = attributes.get(USERKEY);
    }

    return (uKey == null || uKey.isEmpty()) ? null : uKey.get(0);
  }

  public static ClientContext decode(List<String> headers, List<KeyParts> apiKeys) {
    ClientContext strategy = new ClientContext(apiKeys.stream().anyMatch((k) -> k.getServiceKey().contains("*")));

    if (headers != null) {
      for (String header : headers) {
        for (String part : header.split(",")) {
          int pos = part.indexOf("=");
          if (pos != -1) {
            String key = URLDecoder.decode(part.substring(0, pos), StandardCharsets.UTF_8);
            String val = URLDecoder.decode(part.substring(pos + 1), StandardCharsets.UTF_8);
            List<String> vals;
            if (val.contains(",")) {
              vals = Arrays.asList(val.split(","));
            } else {
              vals = Collections.singletonList(val);
            }

            strategy.attributes.put(key, vals);

            // max 30 attributes
            if (strategy.attributes.size() >= 30) {
              return strategy;
            }
          }
        }
      }
    }

    return strategy;
  }

  public String get(String key, String defaultValue) {
    List<String> val = attributes.get(key);

    if (val == null || val.isEmpty()) {
      return defaultValue;
    }

    return val.get(0);
  }

  public String makeEtag() {
    return Integer.toHexString(attributes.hashCode());
  }
}
