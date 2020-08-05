package io.featurehub.client.interceptor;

import io.featurehub.client.FeatureValueInterceptor;

/**
 * Checks system properties for updated features.
 */
public class SystemPropertyValueInterceptor implements FeatureValueInterceptor {
  public static final String FEATURE_TOGGLES_PREFIX = "featurehub.feature.";
  public static final String FEATURE_TOGGLES_ALLOW_OVERRIDE = "featurehub.features.allow-override";

  @Override
  public ValueMatch getValue(String key) {
    String value = null;
    boolean matched = false;

    if (System.getProperty(FEATURE_TOGGLES_ALLOW_OVERRIDE) != null) {
      String k = FEATURE_TOGGLES_PREFIX + key;
      if (System.getProperties().containsKey(k)) {
        matched = true;
        value = System.getProperty(k);
        if (value != null && value.trim().length() == 0) {
          value = null;
        }
      }
    }

    return new ValueMatch(matched, value);
  }
}
