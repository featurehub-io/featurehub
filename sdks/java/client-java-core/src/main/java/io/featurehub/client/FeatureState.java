package io.featurehub.client;

import java.math.BigDecimal;

public interface FeatureState {
  String getKey();

  String getString();

  Boolean getBoolean();

  BigDecimal getNumber();

  String getRawJson();

  <T> T getJson(Class<T> type);

  /**
   * true if the flag is boolean and is true
  */
  boolean isEnabled();

  boolean isSet();

  void addListener(FeatureListener listener);

  FeatureState withContext(ClientContext ctx);
}
