package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Index(unique = true, columnNames = {"fk_fv_id", "fk_rs_id"}, name = "idx_feature_strat")
@Entity
@Table(name = "fh_strat_for_feature")
@ChangeLog
public class DbStrategyForFeatureValue {
  @Id
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_fv_id")
  @Column(nullable = false, name = "fk_fv_id")
  private DbFeatureValue featureValue;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_rs_id")
  @Column(nullable = false, name = "fk_rs_id")
  private DbRolloutStrategy rolloutStrategy;

  @Column(nullable = false, name = "fv_enabled")
  private boolean enabled;

  @Lob
  @Column(name = "fv_value")
  private String value;

  private DbStrategyForFeatureValue(Builder builder) {
    setFeatureValue(builder.featureValue);
    setRolloutStrategy(builder.rolloutStrategy);
    setEnabled(builder.enabled);
    setValue(builder.value);
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

  public DbRolloutStrategy getRolloutStrategy() {
    return rolloutStrategy;
  }

  public void setRolloutStrategy(DbRolloutStrategy rolloutStrategy) {
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

  public static final class Builder {
    private DbFeatureValue featureValue;
    private DbRolloutStrategy rolloutStrategy;
    private boolean enabled;
    private String value;

    public Builder() {
    }

    public Builder featureValue(DbFeatureValue val) {
      featureValue = val;
      return this;
    }

    public Builder rolloutStrategy(DbRolloutStrategy val) {
      rolloutStrategy = val;
      return this;
    }

    public Builder enabled(boolean val) {
      enabled = val;
      return this;
    }

    public Builder value(String val) {
      value = val;
      return this;
    }

    public DbStrategyForFeatureValue build() {
      return new DbStrategyForFeatureValue(this);
    }
  }
}
