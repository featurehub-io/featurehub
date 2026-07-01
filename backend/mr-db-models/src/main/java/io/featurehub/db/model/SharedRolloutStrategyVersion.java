package io.featurehub.db.model;

import java.util.Objects;
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

  // percentage override - as its stored in JSON, we keep it short. This is optional and a new field in 1.11.x +
  @Nullable
  private Integer pOride;

  public SharedRolloutStrategyVersion(@NotNull UUID strategyId, long version, boolean enabled, @Nullable Object value, @Nullable Integer percentage) {
    this.strategyId = strategyId;
    this.version = version;
    this.enabled = enabled;
    this.value = value;
    this.pOride = percentage;
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

  public @Nullable Integer getpOride() {
    return pOride;
  }

  public void setpOride(@Nullable Integer pOride) {
    this.pOride = pOride;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;

    SharedRolloutStrategyVersion that = (SharedRolloutStrategyVersion) o;
    return getVersion() == that.getVersion() && isEnabled() == that.isEnabled() && getStrategyId().equals(that.getStrategyId()) && Objects.equals(getValue(), that.getValue()) && Objects.equals(getpOride(), that.getpOride());
  }

  @Override
  public int hashCode() {
    int result = getStrategyId().hashCode();
    result = 31 * result + Long.hashCode(getVersion());
    result = 31 * result + Boolean.hashCode(isEnabled());
    result = 31 * result + Objects.hashCode(getValue());
    result = 31 * result + Objects.hashCode(getpOride());
    return result;
  }
}
