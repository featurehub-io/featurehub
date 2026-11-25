package io.featurehub.edge;

import io.featurehub.dacha.model.CacheEnvironmentFeature;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.sse.model.FeatureState;

import java.time.Instant;
import java.util.List;

public interface FeatureTransformer {
  default List<FeatureState> transform(List<CacheEnvironmentFeature> features, ClientContext clientAttributes, boolean allowExtendedProperties) {
    return transform(features, clientAttributes, allowExtendedProperties, Instant.now());
  }

  default FeatureState transform(CacheEnvironmentFeature rf, ClientContext clientAttributes, boolean allowExtendedProperties) {
    return transform(rf, clientAttributes, allowExtendedProperties, Instant.now());
  }

  /**
   * @param now the time server evaluation is done at (fhubWhenActive, "now"): one instant for the whole request
   */
  List<FeatureState> transform(List<CacheEnvironmentFeature> features, ClientContext clientAttributes, boolean allowExtendedProperties, Instant now);
  FeatureState transform(CacheEnvironmentFeature rf, ClientContext clientAttributes, boolean allowExtendedProperties, Instant now);
}
