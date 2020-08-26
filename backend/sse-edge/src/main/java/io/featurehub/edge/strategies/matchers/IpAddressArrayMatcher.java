package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;

public class IpAddressArrayMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    switch(attr.getConditional()) {
      case EQUALS:
        return attr.getValues().stream().anyMatch(val -> CIDRMatch.cidrMatch(val.toString(), suppliedValue));
      case ENDS_WITH:
        break;
      case STARTS_WITH:
        break;
      case GREATER:
        break;
      case GREATER_EQUALS:
        break;
      case LESS:
        break;
      case LESS_EQUALS:
        break;
      case NOT_EQUALS:
        return attr.getValues().stream().noneMatch(val -> CIDRMatch.cidrMatch(val.toString(), suppliedValue));
      case INCLUDES:
        return attr.getValues().stream().anyMatch(val -> CIDRMatch.cidrMatch(val.toString(), suppliedValue));
      case EXCLUDES:
        return attr.getValues().stream().noneMatch(val -> CIDRMatch.cidrMatch(val.toString(), suppliedValue));
      case REGEX:
        break;
    }
    return false;
  }
}
