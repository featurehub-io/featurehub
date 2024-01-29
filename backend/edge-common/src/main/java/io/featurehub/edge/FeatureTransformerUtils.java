package io.featurehub.edge;

import io.featurehub.dacha.model.CacheEnvironmentFeature;
import io.featurehub.dacha.model.CacheRolloutStrategy;
import io.featurehub.dacha.model.CacheRolloutStrategyAttribute;
import io.featurehub.edge.strategies.Applied;
import io.featurehub.edge.strategies.ApplyFeature;
import io.featurehub.edge.strategies.ClientContext;
import io.featurehub.sse.model.FeatureRolloutStrategy;
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;
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
  private final ApplyFeature applyFeature =
      new ApplyFeature(new PercentageMumurCalculator(), new MatcherRegistry());

  public List<FeatureState> transform(
      List<CacheEnvironmentFeature> features, ClientContext clientAttributes) {
    try {
      return features.stream()
          .map(f -> transform(f, clientAttributes))
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed transform", e);
      return new ArrayList<>();
    }
  }

  public FeatureState transform(CacheEnvironmentFeature rf, ClientContext clientAttributes) {
    FeatureState fs =
        new FeatureState()
            .key(rf.getFeature().getKey())
            .type(rf.getFeature().getValueType()) // they are the same
            .featureProperties(rf.getFeatureProperties())
            .id(rf.getFeature().getId());


    if (rf.getValue() == null) {
      fs.setVersion(0L);
      fs.setL(false);
    } else {
      fs.setVersion(rf.getValue().getVersion());
      fs.setL(rf.getValue().getLocked());

      if (clientAttributes != null) {
        final List<FeatureRolloutStrategy> fsStrategies =
            (rf.getValue().getRolloutStrategies() != null)
                ? rf.getValue().getRolloutStrategies().stream()
                    .map(this::toFeatureRolloutStrategy)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        if (clientAttributes.isClientEvaluation && rf.getValue() != null) {
          fs.strategies(fsStrategies);
          fs.value(rf.getValue().getValue());
        } else {
          Applied applied =
              applyFeature.applyFeature(
                  fsStrategies,
                  rf.getFeature().getKey(),
                  rf.getValue().getId().toString(),
                  clientAttributes);
          fs.value(
              applied.isMatched()
                  ? applied.getValue()
                  : (rf.getValue() == null ? null : rf.getValue().getValue()));
          // return
          fs.v(applied.getStrategyId());
        }
      }
    }

    return fs;
  }

  private FeatureRolloutStrategy toFeatureRolloutStrategy(CacheRolloutStrategy rs) {
    return new FeatureRolloutStrategy()
        .id(rs.getId())
        .attributes(
            rs.getAttributes().stream()
                .map(this::toFeatureRolloutStrategyAttribute)
                .collect(Collectors.toList()))
        .percentage(rs.getPercentage())
        .percentageAttributes(rs.getPercentageAttributes())
        .value(rs.getValue());
  }

  private FeatureRolloutStrategyAttribute toFeatureRolloutStrategyAttribute(
      CacheRolloutStrategyAttribute rsa) {
    return new FeatureRolloutStrategyAttribute()
        .conditional(rsa.getConditional())
        .type(rsa.getType())
        .fieldName(rsa.getFieldName())
        .values(rsa.getValues());
  }
}
