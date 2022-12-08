package io.featurehub.edge.strategies;

import org.jetbrains.annotations.Nullable;

public class Applied {
  private final boolean matched;
  @Nullable
  private final String strategyId;
  @Nullable
  private final Object value;

  public Applied(boolean matched, @Nullable String strategyId, @Nullable Object value) {
    this.matched = matched;
    this.strategyId = strategyId;
    this.value = value;
  }

  public static Applied noMatch() {
    return new Applied(false, null, null);
  }

  public boolean isMatched() {
    return matched;
  }

  public @Nullable Object getValue() {
    return value;
  }

  public String getStrategyId() {
    return strategyId;
  }
}
