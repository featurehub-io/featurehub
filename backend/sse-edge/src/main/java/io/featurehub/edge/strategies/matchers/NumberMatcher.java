package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;
import io.featurehub.mr.model.RolloutStrategyAttributeConditional;

import java.math.BigDecimal;

public class NumberMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    try {
      BigDecimal bd = new BigDecimal(suppliedValue);

      BigDecimal val = attr.getConditional() == RolloutStrategyAttributeConditional.REGEX ? null : new BigDecimal(attr.getValue().toString());

      switch (attr.getConditional()) {
        case EQUALS:
          return bd.equals(val);
        case ENDS_WITH:
          return false;
        case STARTS_WITH:
          return false;
        case GREATER:
          return bd.compareTo(val) > 0;
        case GREATER_EQUALS:
          return bd.compareTo(val) >= 0;
        case LESS:
          return bd.compareTo(val) < 0;
        case LESS_EQUALS:
          return bd.compareTo(val) <= 0;
        case NOT_EQUALS:
          return bd.compareTo(val) != 0;
        case INCLUDES:
          return false;
        case EXCLUDES:
          return false;
        case REGEX:
          return suppliedValue.matches(attr.getValue().toString());
      }
    } catch (Exception e) {
    }

    return false;
  }
}
