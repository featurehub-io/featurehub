package io.featurehub.strategies.percentage;

import java.nio.charset.StandardCharsets;

public class PercentageMumurCalculator implements PercentageCalculator {
  private final Murmur3_32HashFunction hashFunction;
  public static final int MAX_PERCENTAGE = 1000000;

  public PercentageMumurCalculator(int seed) {
    this.hashFunction = new Murmur3_32HashFunction(seed);
  }

  public PercentageMumurCalculator() {
    this.hashFunction = new Murmur3_32HashFunction(0);
  }

  public int determineClientPercentage(String userKey, String id) {
    int hashCode = hashFunction.hashString(
      (userKey + id), StandardCharsets.UTF_8
    );

    double ratio = (double) (hashCode & 0xFFFFFFFFL) / Math.pow(2, 32);
    return (int) Math.floor(MAX_PERCENTAGE * ratio);
  }
}
