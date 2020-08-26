package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;

public class SemanticVersionMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    final SemanticVersionComparable suppliedVersion = new SemanticVersionComparable(suppliedValue);
    final SemanticVersionComparable val = new SemanticVersionComparable(attr.getValue().toString());

    switch (attr.getConditional()) {
      case EQUALS:
        return suppliedVersion.equals(val);
      case ENDS_WITH:
        break;
      case STARTS_WITH:
        break;
      case GREATER:
        return suppliedVersion.compareTo(val) > 0;
      case GREATER_EQUALS:
        return suppliedVersion.compareTo(val) >= 0;
      case LESS:
        return suppliedVersion.compareTo(val) <= 0;
      case LESS_EQUALS:
        return suppliedVersion.compareTo(val) <= 0;
      case NOT_EQUALS:
        return !suppliedVersion.equals(val);
      case INCLUDES:
        return suppliedVersion.equals(val);
      case EXCLUDES:
        return !suppliedVersion.equals(val);
      case REGEX:
        break;
    }

    return false;
  }
}
