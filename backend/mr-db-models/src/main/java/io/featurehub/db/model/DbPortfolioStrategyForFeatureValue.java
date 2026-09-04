package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import jakarta.persistence.*;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

@Index(unique = true, columnNames = {"fk_fv_id", "fk_prs_id"}, name = "idx_pfeature_strat")
@Entity
@Table(name = "fh_pstrat_for_feature")
@ChangeLog
public class DbPortfolioStrategyForFeatureValue {
  @Id
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_fv_id")
  private DbFeatureValue featureValue;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_prs_id")
  private DbPortfolioRolloutStrategy rolloutStrategy;

  @Column(nullable = false, name = "fv_enabled")
  private boolean enabled;

  @Lob
  @Column(name = "fv_value")
  private String value;

  // this allows the user to override the strategy (merge) when applying it to this feature value
  @Column(name = "percent_oride", nullable = true)
  @Nullable
  private Integer percentageOverride;

  private DbPortfolioStrategyForFeatureValue(DbPortfolioStrategyForFeatureValue.Builder builder) {
    setFeatureValue(builder.featureValue);
    setRolloutStrategy(builder.rolloutStrategy);
    setEnabled(builder.enabled);
    setValue(builder.value);
    setPercentageOverride(builder.percentageOverride);
  }

  public UUID getId() {
    return id;
  }

  public DbFeatureValue getFeatureValue() {
    return featureValue;
  }

  public void setFeatureValue(DbFeatureValue featureValue) {
    this.featureValue = featureValue;
  }

  public DbPortfolioRolloutStrategy getRolloutStrategy() {
    return rolloutStrategy;
  }

  public void setRolloutStrategy(DbPortfolioRolloutStrategy rolloutStrategy) {
    this.rolloutStrategy = rolloutStrategy;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public @Nullable Integer getPercentageOverride() {
    return percentageOverride;
  }

  public void setPercentageOverride(@Nullable Integer percentageOverride) {
    this.percentageOverride = percentageOverride;
  }

  public static final class Builder {
    private DbFeatureValue featureValue;
    private DbPortfolioRolloutStrategy rolloutStrategy;
    private boolean enabled;
    private String value;
    @Nullable
    private Integer percentageOverride;

    public Builder() {
    }

    public Builder percentageOverride(@Nullable Integer percentageOverride) {
      this.percentageOverride = percentageOverride;
      return this;
    }

    public DbPortfolioStrategyForFeatureValue.Builder featureValue(DbFeatureValue val) {
      featureValue = val;
      return this;
    }

    public DbPortfolioStrategyForFeatureValue.Builder rolloutStrategy(DbPortfolioRolloutStrategy val) {
      rolloutStrategy = val;
      return this;
    }

    public DbPortfolioStrategyForFeatureValue.Builder enabled(boolean val) {
      enabled = val;
      return this;
    }

    public DbPortfolioStrategyForFeatureValue.Builder value(String val) {
      value = val;
      return this;
    }

    public DbPortfolioStrategyForFeatureValue build() {
      return new DbPortfolioStrategyForFeatureValue(this);
    }
  }
}
