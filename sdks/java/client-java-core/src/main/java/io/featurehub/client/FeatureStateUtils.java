package io.featurehub.client;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FeatureStateUtils {

  static boolean changed(Object oldValue, Object newValue) {
    return ((oldValue != null && newValue == null) || (newValue != null && oldValue == null) ||
      (oldValue != null && !oldValue.equals(newValue)) || (newValue != null && !newValue.equals(oldValue)));
  }

  public static String generateXFeatureHubHeaderFromMap(Map<String, List<String>> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    }

    return attributes.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(),
     URLEncoder.encode(String.join(",", e.getValue())))).sorted().collect(Collectors.joining(","));
  }

  static boolean isActive(FeatureRepository repository, Feature feature) {
    if (repository == null) {
      throw new RuntimeException("You must configure your feature repository before using it.");
    }

    FeatureState fs = repository.getFeatureState(feature.name());
    return Boolean.TRUE.equals(fs.getBoolean());
  }

  static boolean exists(FeatureRepository repository, Feature feature) {
    FeatureState fs = repository.getFeatureState(feature.name());
    return ((FeatureStateBase)fs).exists();
  }

  static boolean isSet(FeatureRepository repository, Feature feature) {
    return repository.getFeatureState(feature.name()).isSet();
  }

  static void addListener(FeatureRepository repository, Feature feature, FeatureListener featureListener) {
    repository.getFeatureState(feature.name()).addListener(featureListener);
  }
}
