package io.featurehub.db.model;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SharedRolloutStrategyVersion {
  @NotNull
  private UUID strategyId;
  private long version;

  private boolean enabled;

  @Nullable
  private Object value;

  public SharedRolloutStrategyVersion(@NotNull UUID strategyId, long version, boolean enabled, @Nullable Object value) {
    this.strategyId = strategyId;
    this.version = version;
    this.enabled = enabled;
    this.value = value;
  }

  public SharedRolloutStrategyVersion() {}

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

  public void setStrategyId(@NotNull UUID strategyId) {
    this.strategyId = strategyId;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public void setValue(@Nullable Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SharedRolloutStrategyVersion that = (SharedRolloutStrategyVersion) o;

    if (getVersion() != that.getVersion()) return false;
    if (isEnabled() != that.isEnabled()) return false;
    if (!getStrategyId().equals(that.getStrategyId())) return false;
      return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() == null;
  }

  @Override
  public int hashCode() {
    int result = getStrategyId().hashCode();
    result = 31 * result + (int) (getVersion() ^ (getVersion() >>> 32));
    result = 31 * result + (isEnabled() ? 1 : 0);
    result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
    return result;
  }
}
