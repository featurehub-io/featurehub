package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

public class SemanticVersionArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    final SemanticVersionComparable suppliedVersion = new SemanticVersionComparable(suppliedValue);

    switch (attr.getConditional()) {
      case EQUALS:
        return attr.getValues().stream().anyMatch(v -> suppliedVersion.equals(new SemanticVersionComparable(v.toString())));
      case ENDS_WITH:
        break;
      case STARTS_WITH:
        break;
      case GREATER:
        return attr.getValues().stream().anyMatch(v -> suppliedVersion.compareTo(new SemanticVersionComparable(v.toString())) > 0);
      case GREATER_EQUALS:
        return attr.getValues().stream().anyMatch(v -> suppliedVersion.compareTo(new SemanticVersionComparable(v.toString())) >= 0);
      case LESS:
        return attr.getValues().stream().anyMatch(v -> suppliedVersion.compareTo(new SemanticVersionComparable(v.toString())) < 0);
      case LESS_EQUALS:
        return attr.getValues().stream().anyMatch(v -> suppliedVersion.compareTo(new SemanticVersionComparable(v.toString())) >= 0);
      case NOT_EQUALS:
        return attr.getValues().stream().noneMatch(v -> suppliedVersion.equals(new SemanticVersionComparable(v.toString())));
      case INCLUDES:
        return attr.getValues().stream().anyMatch(v -> suppliedVersion.equals(new SemanticVersionComparable(v.toString())));
      case EXCLUDES:
        return attr.getValues().stream().noneMatch(v -> suppliedVersion.equals(new SemanticVersionComparable(v.toString())));
      case REGEX:
        break;
    }

    return false;
  }
}
