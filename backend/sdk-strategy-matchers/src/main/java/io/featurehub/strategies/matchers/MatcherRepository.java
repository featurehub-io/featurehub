package io.featurehub.strategies.matchers;


import io.featurehub.mr.model.RolloutStrategyAttribute;

public interface MatcherRepository {
  StrategyMatcher findMatcher(RolloutStrategyAttribute attr);
}
