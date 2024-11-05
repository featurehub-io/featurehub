package io.featurehub.db.model;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SharedRolloutStrategyVersion {
  @NotNull
  private final UUID strategyId;
  private final long version;

  private final boolean enabled;

  @Nullable
  private final  Object value;

  public SharedRolloutStrategyVersion(@NotNull UUID strategyId, long version, boolean enabled, @Nullable Object value) {
    this.strategyId = strategyId;
    this.version = version;
    this.enabled = enabled;
    this.value = value;
  }

  public @NotNull UUID getStrategyId() {
    return strategyId;
  }

  public long getVersion() {
    return version;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public @Nullable Object getValue() {
    return value;
  }
}
