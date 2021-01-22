package io.featurehub.client;

import java.math.BigDecimal;

public interface FeatureStateHolder {
  String getKey();

  String getString();

  Boolean getBoolean();

  BigDecimal getNumber();

  String getRawJson();

  <T> T getJson(Class<T> type);

  boolean isSet();

  void addListener(FeatureListener listener);

  FeatureStateHolder withContext(ClientContext ctx);
}
