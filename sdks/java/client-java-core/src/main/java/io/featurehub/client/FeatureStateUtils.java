package io.featurehub.client;

public class FeatureStateUtils {

  static boolean changed(Object oldValue, Object newValue) {
    return ((oldValue != null && newValue == null) || (newValue != null && oldValue == null) ||
      (oldValue != null && !oldValue.equals(newValue)) || (newValue != null && !newValue.equals(oldValue)));
  }

  static boolean isActive(FeatureRepository repository, Feature feature) {
    if (repository == null) {
      throw new RuntimeException("You must configure your feature repository before using it.");
    }

    FeatureStateHolder fs = repository.getFeatureState(feature.name());
    return Boolean.TRUE.equals(fs.getBoolean());
  }

  static boolean exists(FeatureRepository repository, Feature feature) {
    FeatureStateHolder fs = repository.getFeatureState(feature.name());
    return !(fs instanceof FeatureStateBaseHolder);
  }

  static boolean isSet(FeatureRepository repository, Feature feature) {
    return repository.getFeatureState(feature.name()).isSet();
  }

  static void addListener(FeatureRepository repository, Feature feature, FeatureListener featureListener) {
    repository.getFeatureState(feature.name()).addListener(featureListener);
  }
}
