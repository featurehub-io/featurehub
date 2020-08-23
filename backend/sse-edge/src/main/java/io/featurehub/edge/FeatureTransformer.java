package io.featurehub.edge;

import io.featurehub.edge.strategies.ClientAttributeCollection;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.sse.model.FeatureState;

import java.util.List;

public interface FeatureTransformer {
  List<FeatureState> transform(List<FeatureValueCacheItem> features, ClientAttributeCollection clientAttributes);
  FeatureState transform(FeatureValueCacheItem rf, ClientAttributeCollection clientAttributes);
}
