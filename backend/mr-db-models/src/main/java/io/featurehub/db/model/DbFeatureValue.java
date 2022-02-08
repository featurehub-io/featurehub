package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.featurehub.mr.model.RolloutStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "fh_env_feature_strategy")
@Index(name = "idx_fv_unique", unique = true, columnNames = {"fk_environment_id", "fk_feature_id"})
@ChangeLog
public class DbFeatureValue extends DbVersionedBase {
  private DbFeatureValue(Builder builder) {
    setWhoUpdated(builder.whoUpdated);
    setWhatUpdated(builder.whatUpdated);
    setEnvironment(builder.environment);
    setFeature(builder.feature);
    setFeatureState(builder.featureState);
    setDefaultValue(builder.defaultValue);
    setLocked(builder.locked);
    setRolloutStrategies(builder.rolloutStrategies);
    setRetired(builder.retired);
    sharedRolloutStrategies = builder.sharedRolloutStrategies;
  }

  @ManyToOne
  @Column(name = "fk_who_updated", nullable = true)
  @JoinColumn(name = "fk_who_updated")
  @DbForeignKey(onDelete = ConstraintMode.SET_NULL)
  private DbPerson whoUpdated;

  @Column(length = 400)
  private String whatUpdated;

  public void setWhatUpdated(String whatUpdated) {
    this.whatUpdated = whatUpdated;
  }

  public String getWhatUpdated() {
    return whatUpdated;
  }

  // in sql, create a unique index on these two

  @ManyToOne(optional = false)
  @Column(name = "fk_environment_id", nullable = false)
  @JoinColumn(name = "fk_environment_id")
  private DbEnvironment environment;

  @ManyToOne(optional = false)
  @Column(name = "fk_feature_id")
  @JoinColumn(name = "fk_feature_id")
  private DbApplicationFeature feature;

  @Enumerated(value = EnumType.STRING)
  private FeatureState featureState;

  @Lob
  private String defaultValue;

  @Column(nullable = false)
  private boolean locked;

  /**
   * When null or false, it is not retired, when true it is retired and Edge won't see it.
   */
  @Column
  private Boolean retired;

  // a user can have multiple strategies here that are specific to this feature value
  // these are usually percentage only ones, but that may change in the future
  @DbJson
  @Column(name="rollout_strat")
  private List<RolloutStrategy> rolloutStrategies;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_fv_id")
  private List<DbStrategyForFeatureValue> sharedRolloutStrategies;

  public DbFeatureValue() {
  }

  public List<DbStrategyForFeatureValue> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }

  public Boolean getRetired() {
    return retired;
  }

  public void setRetired(Boolean retired) {
    this.retired = retired;
  }

  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
  }

  public DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(DbEnvironment environment) {
    this.environment = environment;
  }

  public DbApplicationFeature getFeature() {
    return feature;
  }

  public void setFeature(DbApplicationFeature feature) {
    this.feature = feature;
  }

  public FeatureState getFeatureState() {
    return featureState;
  }

  public void setFeatureState(FeatureState featureState) {
    this.featureState = featureState;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public List<RolloutStrategy> getRolloutStrategies() {
    return rolloutStrategies;
  }

  public void setRolloutStrategies(List<RolloutStrategy> rolloutStrategies) {
    this.rolloutStrategies = rolloutStrategies;
  }

  public static final class Builder {
    private DbPerson whoUpdated;
    private String whatUpdated;
    private DbEnvironment environment;
    private DbApplicationFeature feature;
    private FeatureState featureState;
    private String defaultValue;
    private boolean locked;
    private Boolean retired; // null == true
    private List<RolloutStrategy> rolloutStrategies;
    private List<DbStrategyForFeatureValue> sharedRolloutStrategies;

    public Builder() {
    }

    public Builder retired(Boolean retired) {
      this.retired = retired;
      return this;
    }

    public Builder whoUpdated(DbPerson val) {
      whoUpdated = val;
      return this;
    }

    public Builder whatUpdated(String val) {
      whatUpdated = val;
      return this;
    }

    public Builder environment(DbEnvironment val) {
      environment = val;
      return this;
    }

    public Builder feature(DbApplicationFeature val) {
      feature = val;
      return this;
    }

    public Builder featureState(FeatureState val) {
      featureState = val;
      return this;
    }

    public Builder defaultValue(String val) {
      defaultValue = val;
      return this;
    }

    public Builder locked(boolean val) {
      locked = val;
      return this;
    }

    public Builder rolloutStrategies(List<RolloutStrategy> val) {
      rolloutStrategies = val;
      return this;
    }

    public Builder sharedRolloutStrategies(List<DbStrategyForFeatureValue> val) {
      sharedRolloutStrategies = val;
      return this;
    }

    public DbFeatureValue build() {
      return new DbFeatureValue(this);
    }
  }


}
