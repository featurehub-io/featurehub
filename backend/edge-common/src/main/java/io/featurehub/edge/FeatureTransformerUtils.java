package io.featurehub.edge;

import io.featurehub.edge.strategies.Applied;
import io.featurehub.edge.strategies.ApplyFeature;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.strategies.matchers.MatcherRegistry;
import io.featurehub.strategies.percentage.PercentageMumurCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FeatureTransformerUtils implements FeatureTransformer {
  private static final Logger log = LoggerFactory.getLogger(FeatureTransformerUtils.class);
  private final ApplyFeature applyFeature = new ApplyFeature(new PercentageMumurCalculator(), new MatcherRegistry());

  public List<FeatureState> transform(List<FeatureValueCacheItem> features, ClientContext clientAttributes) {
    try {
      return features.stream().map(f -> transform(f, clientAttributes)).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed transform", e);
      return new ArrayList<>();
    }
  }

  public FeatureState transform(FeatureValueCacheItem rf, ClientContext clientAttributes) {
    FeatureState fs = new FeatureState()
//      .key(rf.getFeature().getAlias() != null ? rf.getFeature().getAlias() : rf.getFeature().getKey())
      .key(rf.getFeature().getKey())
      .type(rf.getFeature().getValueType()) // they are the same
      .id(rf.getFeature().getId())
      .l(rf.getValue().getLocked());

    if (rf.getValue() == null || rf.getValue().getVersion() == null) {
      fs.setVersion(0L);
    } else {
      fs.setVersion(rf.getValue().getVersion());
    }

    if (clientAttributes != null) {
      if (clientAttributes.isClientEvaluation) {
        fs.strategies(rf.getStrategies());
        fs.value(valueAsObject(rf));
      } else {
        Applied applied = applyFeature.applyFeature(rf.getStrategies(), rf.getFeature().getKey(), rf.getValue().getId().toString()
          , clientAttributes);
        fs.value(applied.isMatched() ? applied.getValue() : valueAsObject(rf));
      }
    }

    return fs;
  }

  private Object valueAsObject(FeatureValueCacheItem rf) {
    if (rf.getValue() == null)
      return null;

    final FeatureValueType valueType = rf.getFeature().getValueType();
    if (FeatureValueType.BOOLEAN.equals(valueType)) {
      return rf.getValue().getValueBoolean();
    }

    if (FeatureValueType.JSON.equals(valueType)) {
      return rf.getValue().getValueJson();
    }

    if ( FeatureValueType.STRING.equals(valueType)) {
      return rf.getValue().getValueString();
    }

    if (FeatureValueType.NUMBER.equals(valueType)) {
      return rf.getValue().getValueNumber();
    }

    log.error("unknown feature value type, sending null: {}: {}", rf.getFeature().getId(), valueType);

    return null;
  }

}
