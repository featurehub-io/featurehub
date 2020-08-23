package io.featurehub.edge.strategies;

public interface PercentageCalculator {
  int determineClientPercentage(String userKey, String id);
}
