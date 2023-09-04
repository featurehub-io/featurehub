package io.featurehub.dacha;

import io.featurehub.dacha.model.CacheEnvironmentFeature;
import io.featurehub.dacha.model.PublishEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnvironmentFeatures implements InternalCache.FeatureValues {
  private final PublishEnvironment env;
  // Feature::id, CacheFeatureValue
  private final Map<UUID, CacheEnvironmentFeature> features;
  private String etag;

  public EnvironmentFeatures(PublishEnvironment env) {
    this.env = env;

    this.features = new ConcurrentHashMap<>(env.getFeatureValues().stream()
      .collect(Collectors.toMap(f -> f.getFeature().getId(), Function.identity())));

    calculateEtag();
  }

  public void calculateEtag() {
    String val = features.values().stream()
      .map(fvci -> fvci.getFeature().getId() + "-" + (fvci.getValue() == null ? "0000" :  fvci.getValue().getVersion()))
      .collect(Collectors.joining("-"));
    etag =
        Integer.toHexString(val.hashCode());
  }

  // the UUID is the FEATURE's UUID NOT the feature value's one
  public CacheEnvironmentFeature get(UUID id) {
    return features.get(id);
  }

  public void set(CacheEnvironmentFeature feature) {
    features.put(feature.getFeature().getId(), feature);

    // modify the copy so no sync issues
    final List<CacheEnvironmentFeature> featureValues = new ArrayList<>(env.getFeatureValues());

    featureValues.removeIf((f) -> f.getFeature().getId().equals(feature.getFeature().getId()));
    featureValues.add(feature);

    env.setFeatureValues(featureValues);
    calculateEtag();
  }

  public void remove(UUID id) {
    features.remove(id);
    final List<CacheEnvironmentFeature> featureValues = new ArrayList<>(env.getFeatureValues());
    featureValues.removeIf((f) -> f.getFeature().getId().equals(id));
    env.setFeatureValues(featureValues);
    calculateEtag();
  }

  @Override
  public Collection<CacheEnvironmentFeature> getFeatures() {
    return features.values();
  }

  @Override
  public PublishEnvironment getEnvironment() {
    return env;
  }

  @Override
  public String getEtag() {
    return etag;
  }

  @Override
  public String toString() {
    final List<String> collect =
      features.values().stream().map(CacheEnvironmentFeature::toString).collect(Collectors.toList());
    return String.format("etag: %s, features %s", etag == null ? "null" : etag, String.join(",",
      collect));
  }
}
