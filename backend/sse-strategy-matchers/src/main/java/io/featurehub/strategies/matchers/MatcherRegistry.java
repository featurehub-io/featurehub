package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

public class MatcherRegistry implements MatcherRepository {
  @Override
  public StrategyMatcher findMatcher(String suppliedValue, RolloutStrategyAttribute attr) {
    boolean isArray = Boolean.TRUE.equals(attr.getArray());

    switch (attr.getType()) {
      case STRING:
        return isArray ? new StringArrayMatcher() : new StringMatcher();
      case SEMANTIC_VERSION:
        return isArray ? new SemanticVersionArrayMatcher() : new SemanticVersionMatcher();
      case NUMBER:
        return isArray ? new NumberArrayMatcher() : new NumberMatcher();
      case DATE:
        return isArray ? new DateArrayMatcher() : new DateMatcher();
      case DATETIME:
        return isArray ? new DateTimeArrayMatcher() : new DateTimeMatcher();
      case BOOLEAN: // can't have arrays, that would be silly
        return new BooleanMatcher();
      case IP_ADDRESS:
        return isArray ? new IpAddressArrayMatcher() : new IpAddressMatcher();
    }

    return new FallthroughMatcher();
  }

  static class FallthroughMatcher implements StrategyMatcher {
    @Override
    public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
      return false;
    }
  }
}
