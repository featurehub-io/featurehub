package io.featurehub.client;

import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.SSEResultState;

import java.util.List;
import java.util.Map;

public interface FeatureRepository {
  void addAnalyticCollector(AnalyticsCollector collector);

  void notify(SSEResultState state, String data);

  void notify(List<FeatureState> states);

  void addReadynessListener(ReadynessListener rl);

  FeatureStateHolder getFeatureState(String key);

  void logAnalyticsEvent(String action, Map<String, String> other);
}
