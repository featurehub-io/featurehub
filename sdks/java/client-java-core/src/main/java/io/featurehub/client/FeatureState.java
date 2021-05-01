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

  boolean isLocked();

  /**
   * Adds a listener to a feature. Do *not* add a listener to a context in server mode, where you are creating
   * lots of contexts as this will lead to a memory leak.
   * @param listener
   */
  void addListener(FeatureListener listener);

  FeatureState withContext(ClientContext ctx);
}
