package io.featurehub.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;
import io.featurehub.mr.model.RolloutStrategyAttributeConditional;

public class BooleanArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
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
