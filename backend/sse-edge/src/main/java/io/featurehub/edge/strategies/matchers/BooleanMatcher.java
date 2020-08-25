package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;
import io.featurehub.mr.model.RolloutStrategyAttributeConditional;

public class BooleanMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    boolean val = "true".equals(suppliedValue);

    if (attr.getConditional() == RolloutStrategyAttributeConditional.EQUALS) {
      return val == (Boolean)attr.getValue();
    }

    if (attr.getConditional() == RolloutStrategyAttributeConditional.NOT_EQUALS) {
      return val == !(Boolean)attr.getValue();
    }

    return false;
  }
}
