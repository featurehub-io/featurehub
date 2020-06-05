package io.featurehub.client;

import java.util.List;
import java.util.Map;

public interface AnalyticsCollector {
  void logEvent(String action, Map<String, String> other, List<FeatureStateHolder> featureStateAtCurrentTime);
}
