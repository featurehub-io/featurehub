package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

public class MatcherRegistry implements MatcherRepository {
  @Override
  public StrategyMatcher findMatcher(String suppliedValue, RolloutStrategyAttribute attr) {
    switch (attr.getType()) {
      case STRING:
        return new StringArrayMatcher();
      case SEMANTIC_VERSION:
        return new SemanticVersionArrayMatcher();
      case NUMBER:
        return new NumberArrayMatcher();
      case DATE:
        return new DateArrayMatcher();
      case DATETIME:
        return new DateTimeArrayMatcher();
      case BOOLEAN: // can't have arrays, that would be silly
        return new BooleanArrayMatcher();
      case IP_ADDRESS:
        return new IpAddressArrayMatcher();
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
