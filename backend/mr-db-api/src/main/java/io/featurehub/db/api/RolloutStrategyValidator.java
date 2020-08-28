package io.featurehub.db.api;

import io.featurehub.mr.model.RolloutStrategy;

import java.util.List;

public interface RolloutStrategyValidator {

  void validateStrategies(List<RolloutStrategy> rolloutStrategies)
    throws InvalidStrategyCombination, PercentageStrategyGreaterThan100Percent;

  static class InvalidStrategyCombination extends Exception {}
  static class PercentageStrategyGreaterThan100Percent extends Exception {}

}
