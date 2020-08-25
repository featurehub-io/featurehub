package io.featurehub.edge.strategies.matchers;

import io.featurehub.mr.model.RolloutStrategyAttribute;

public interface MatcherRepository {
  StrategyMatcher findMatcher(String suppliedValue, RolloutStrategyAttribute attr);
}
