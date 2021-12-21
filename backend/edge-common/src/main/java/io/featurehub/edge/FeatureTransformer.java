package io.featurehub.edge;

import io.featurehub.dacha.model.CacheEnvironmentFeature;
import io.featurehub.dacha.model.CacheFeatureValue;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.sse.model.FeatureState;

import java.util.List;

public interface FeatureTransformer {
  List<FeatureState> transform(List<CacheEnvironmentFeature> features, ClientContext clientAttributes);
  FeatureState transform(CacheEnvironmentFeature rf, ClientContext clientAttributes);
}
