package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;

import java.time.format.DateTimeFormatter;

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
        return isArray ? new DateArrayMatcher(DateTimeFormatter.ISO_DATE) : new DateMatcher(DateTimeFormatter.ISO_DATE);
      case DATETIME:
        return isArray ? new DateArrayMatcher(DateTimeFormatter.ISO_DATE_TIME) : new DateMatcher(DateTimeFormatter.ISO_DATE_TIME);
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
