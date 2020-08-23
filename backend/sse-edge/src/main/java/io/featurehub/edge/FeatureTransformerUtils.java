package io.featurehub.edge;

import io.featurehub.edge.strategies.ApplyFeature;
import io.featurehub.edge.strategies.ClientAttributeCollection;
import io.featurehub.edge.strategies.PercentageMumurCalculator;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.sse.model.FeatureState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class FeatureTransformerUtils implements FeatureTransformer {
  private static final Logger log = LoggerFactory.getLogger(FeatureTransformerUtils.class);
  private final ApplyFeature applyFeature = new ApplyFeature(new PercentageMumurCalculator());

  public List<FeatureState> transform(List<FeatureValueCacheItem> features, ClientAttributeCollection clientAttributes) {
    return features.stream().map(f -> transform(f, clientAttributes)).collect(Collectors.toList());
  }

  public FeatureState transform(FeatureValueCacheItem rf, ClientAttributeCollection clientAttributes) {

    // todo: should also do rollout strategy
    FeatureState fs = new FeatureState()
//      .key(rf.getFeature().getAlias() != null ? rf.getFeature().getAlias() : rf.getFeature().getKey())
      .key(rf.getFeature().getKey())
      .type(io.featurehub.sse.model.FeatureValueType.fromValue(rf.getFeature().getValueType().toString())) // they are the same
      .id(rf.getFeature().getId())
      .l(rf.getValue().getLocked())
      .value(applyFeature.applyFeature(rf, clientAttributes));

    if (rf.getValue() == null || rf.getValue().getVersion() == null) {
      fs.setVersion(0L);
    } else {
      fs.setVersion(rf.getValue().getVersion());
    }

    return fs;
  }



}
