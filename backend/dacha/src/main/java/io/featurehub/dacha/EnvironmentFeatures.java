package io.featurehub.dacha;

import io.featurehub.mr.model.FeatureValueCacheItem;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

class EnvironmentFeatures implements InternalCache.FeatureValues {
  private final Map<UUID, FeatureValueCacheItem> features;
  private String etag;

  public EnvironmentFeatures(Map<UUID, FeatureValueCacheItem> features) {
    this.features = features;

    calculateEtag();
  }

  private void calculateEtag() {
    etag =
        Integer.toHexString(
            features.values().stream()
                .map(fvci -> fvci.getFeature().getId() + "-" + fvci.getFeature().getVersion())
                .collect(Collectors.joining("-"))
                .hashCode());
  }

  public FeatureValueCacheItem get(UUID id) {
    return features.get(id);
  }

  public void put(UUID id, FeatureValueCacheItem feature) {
    features.put(id, feature);
    calculateEtag();
  }

  public void remove(UUID id) {
    features.remove(id);
    calculateEtag();
  }

  @Override
  public Collection<FeatureValueCacheItem> getFeatures() {
    return features.values();
  }

  @Override
  public String getEtag() {
    return etag;
  }
}
