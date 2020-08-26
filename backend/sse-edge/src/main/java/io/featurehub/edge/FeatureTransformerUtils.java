package io.featurehub.edge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.featurehub.dacha.api.CacheJsonMapper;
import io.featurehub.edge.strategies.Applied;
import io.featurehub.edge.strategies.ApplyFeature;
import io.featurehub.edge.strategies.ClientAttributeCollection;
import io.featurehub.strategies.percentage.PercentageMumurCalculator;
import io.featurehub.strategies.matchers.MatcherRegistry;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.sse.model.FeatureState;
import io.featurehub.sse.model.RolloutStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FeatureTransformerUtils implements FeatureTransformer {
  private static final Logger log = LoggerFactory.getLogger(FeatureTransformerUtils.class);
  private final ApplyFeature applyFeature = new ApplyFeature(new PercentageMumurCalculator(), new MatcherRegistry());

  public List<FeatureState> transform(List<FeatureValueCacheItem> features, ClientAttributeCollection clientAttributes) {
    return features.stream().map(f -> transform(f, clientAttributes)).collect(Collectors.toList());
  }

  public FeatureState transform(FeatureValueCacheItem rf, ClientAttributeCollection clientAttributes) {


    FeatureState fs = new FeatureState()
//      .key(rf.getFeature().getAlias() != null ? rf.getFeature().getAlias() : rf.getFeature().getKey())
      .key(rf.getFeature().getKey())
      .type(io.featurehub.sse.model.FeatureValueType.fromValue(rf.getFeature().getValueType().toString())) // they are the same
      .id(rf.getFeature().getId())
      .l(rf.getValue().getLocked());

    List<RolloutStrategy> clientStrategies = transformStrategies(rf.getStrategies());
    if (clientAttributes != null && clientAttributes.hasAttributes()) {
      Applied applied = applyFeature.applyFeature(clientStrategies, rf.getFeature().getKey(), rf.getValue().getId()
        , clientAttributes);
      fs.value(applied.isMatched() ? applied.getValue() : valueAsObject(rf));
    } else {
      fs.strategies(clientStrategies);
      fs.value(valueAsObject(rf));
    }


    if (rf.getValue() == null || rf.getValue().getVersion() == null) {
      fs.setVersion(0L);
    } else {
      fs.setVersion(rf.getValue().getVersion());
    }

    return fs;
  }

  private static final TypeReference<List<RolloutStrategy>> ROLLOUT_TYPE = new TypeReference<List<RolloutStrategy>>(){};

  // these are exactly the same class and from a maintenance perspective this is more sensible.
  private List<RolloutStrategy> transformStrategies(List<io.featurehub.mr.model.RolloutStrategy> strategies) {
    try {
      return CacheJsonMapper.mapper.readValue(CacheJsonMapper.mapper.writeValueAsString(strategies), ROLLOUT_TYPE);
    } catch (JsonProcessingException e) {
      return new ArrayList<>();
    }
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
