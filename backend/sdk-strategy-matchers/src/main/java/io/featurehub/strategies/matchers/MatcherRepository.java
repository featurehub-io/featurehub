package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.RolloutStrategyAttribute;

public interface MatcherRepository {
  StrategyMatcher findMatcher(RolloutStrategyAttribute attr);
}
