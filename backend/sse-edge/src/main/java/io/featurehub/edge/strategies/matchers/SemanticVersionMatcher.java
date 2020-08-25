package io.featurehub.edge.strategies.matchers;

import io.featurehub.edge.strategies.matchers.StrategyMatcher;
import io.featurehub.mr.model.RolloutStrategyAttribute;

public class SemanticVersionMatcher implements StrategyMatcher {
  @Override
  public boolean match(String suppliedValue, RolloutStrategyAttribute attr) {
    return false;
  }
}
