package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;

public class StringMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    final String val = attr.getValue().toString();

    switch (attr.getConditional()) {
      case EQUALS:
        return attr.getValue().equals(suppliedValue);
      case ENDS_WITH:
        return suppliedValue.endsWith(val);
      case STARTS_WITH:
        return suppliedValue.startsWith(val);
      case GREATER:
        return suppliedValue.compareTo(val) > 0;
      case GREATER_EQUALS:
        return suppliedValue.compareTo(val) >= 0;
      case LESS:
        return suppliedValue.compareTo(val) < 0;
      case LESS_EQUALS:
        return suppliedValue.compareTo(val) <= 0;
      case NOT_EQUALS:
        return !suppliedValue.equals(val);
      case INCLUDES:
        return suppliedValue.contains(val);
      case EXCLUDES:
        return !suppliedValue.contains(val);
      case REGEX:
        return suppliedValue.matches(attr.getValue().toString());
    }

    return false;

  }
}
