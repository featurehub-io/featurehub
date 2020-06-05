package io.featurehub.client;

public class FeatureStateUtils {

  static boolean changed(Object oldValue, Object newValue) {
    return ((oldValue != null && newValue == null) || (newValue != null && oldValue == null) ||
      (oldValue != null && !oldValue.equals(newValue)) || (newValue != null && !newValue.equals(oldValue)));
  }

  static boolean isActive(ClientFeatureRepository repository, Feature feature) {
    if (repository == null) {
      throw new RuntimeException("You must configure your feature repository before using it.");
    }

    FeatureStateHolder fs = repository.getFeatureState(feature.name());
    return Boolean.TRUE.equals(fs.getBoolean());
  }

  static boolean exists(ClientFeatureRepository repository, Feature feature) {
    FeatureStateHolder fs = repository.getFeatureState(feature.name());
    return !(fs instanceof FeatureStateBaseHolder);
  }

  static boolean isSet(ClientFeatureRepository repository, Feature feature) {
    return repository.getFeatureState(feature.name()).isSet();
  }

  static void addListener(ClientFeatureRepository repository, Feature feature, FeatureListener featureListener) {
    repository.getFeatureState(feature.name()).addListener(featureListener);
  }
}
