package io.featurehub.strategies.matchers;

import io.featurehub.sse.model.FeatureRolloutStrategyAttribute;

public interface StrategyMatcher {
  boolean match(String suppliedValue, FeatureRolloutStrategyAttribute attr);
}
