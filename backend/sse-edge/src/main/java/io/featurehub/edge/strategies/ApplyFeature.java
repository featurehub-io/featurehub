package io.featurehub.edge.strategies;

import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.RolloutStrategyInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ApplyFeature {
  private static final Logger log = LoggerFactory.getLogger(ApplyFeature.class);
  private final PercentageCalculator percentageCalculator;

  @Inject
  public ApplyFeature(PercentageCalculator percentageCalculator) {
    this.percentageCalculator = percentageCalculator;
  }

  public Object applyFeature(FeatureValueCacheItem item, ClientAttributeCollection cac) {
    if (cac != null & item.getValue().getRolloutStrategyInstances() != null && !item.getValue().getRolloutStrategyInstances().isEmpty()) {
      Integer percentage = null;
      int basePercentage = 0;
      String userKey = cac.userKey();
      for(RolloutStrategyInstance rsi : item.getValue().getRolloutStrategyInstances() ) {
        if (rsi.getPercentage() != null && userKey != null) {
          if (percentage == null) {
            percentage = percentageCalculator.determineClientPercentage(cac.userKey(), item.getValue().getId());
            log.info("percentage for {} on {} calculated at {}", cac.userKey(), item.getFeature().getKey(), percentage);
          }

          log.info("comparing actual {} vs required: {}", percentage, rsi.getPercentage());
          int useBasePercentage = rsi.getAttributes() == null || rsi.getAttributes().isEmpty() ? basePercentage : 0;
            // if the percentage is lower than the user's key +
            // id of feature value then apply it
          if (percentage <= (useBasePercentage + rsi.getPercentage())) {
            return applyRolloutStrategy(rsi, item.getFeature().getValueType());
          }

          // this was only a percentage and had no other attributes
          if (rsi.getAttributes() == null || rsi.getAttributes().isEmpty()) {
            basePercentage += rsi.getPercentage();
          }
        }
      }
    }

    return valueAsObject(item);
  }



  private Object applyRolloutStrategy(RolloutStrategyInstance rsi, FeatureValueType valueType) {
    if (valueType == FeatureValueType.BOOLEAN) {
      return rsi.getValueBoolean();
    }

    if (valueType == FeatureValueType.JSON) {
      return rsi.getValueJson();
    }

    if (valueType == FeatureValueType.NUMBER) {
      return rsi.getValueNumber();
    }

    if (valueType == FeatureValueType.STRING) {
      return rsi.getValueString();
    }

    return null;
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
