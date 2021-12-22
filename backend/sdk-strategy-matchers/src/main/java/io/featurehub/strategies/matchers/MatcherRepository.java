package io.featurehub.strategies.matchers;


import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

public interface MatcherRepository {
  StrategyMatcher findMatcher(FeatureRolloutStrategyAttribute attr);
}
