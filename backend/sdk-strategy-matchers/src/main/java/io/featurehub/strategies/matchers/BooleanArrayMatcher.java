package io.featurehub.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttributeConditional;
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

public class BooleanArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, FeatureRolloutStrategyAttribute attr) {
    boolean val = "true".equals(suppliedValue);

    if (attr.getConditional() == RolloutStrategyAttributeConditional.EQUALS) {
      return val == (Boolean)attr.getValues().get(0);
    }

    if (attr.getConditional() == RolloutStrategyAttributeConditional.NOT_EQUALS) {
      return val == !(Boolean)attr.getValues().get(0);
    }

    return false;
  }
}
