package io.featurehub.edge;

import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.sse.model.FeatureState;

import java.util.List;

public interface FeatureTransformer {
  List<FeatureState> transform(List<FeatureValueCacheItem> features, ClientContext clientAttributes);
  FeatureState transform(FeatureValueCacheItem rf, ClientContext clientAttributes);
}
