package io.featurehub.strategies.percentage;

public interface PercentageCalculator {
  int determineClientPercentage(String userKey, String id);
}
