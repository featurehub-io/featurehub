package io.featurehub.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;

public interface StrategyMatcher {
  boolean match(String suppliedValue, RolloutStrategyAttribute attr);
}
