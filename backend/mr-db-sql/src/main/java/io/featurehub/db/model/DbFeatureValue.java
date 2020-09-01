package io.featurehub.db.model;

import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.featurehub.mr.model.RolloutStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fh_env_feature_strategy")
public class DbFeatureValue {
  @Id
  private UUID id;

  private DbFeatureValue(Builder builder) {
    setWhoUpdated(builder.whoUpdated);
    setWhatUpdated(builder.whatUpdated);
    setEnvironment(builder.environment);
    setFeature(builder.feature);
    setFeatureState(builder.featureState);
    setDefaultValue(builder.defaultValue);
    setLocked(builder.locked);
    setRolloutStrategies(builder.rolloutStrategies);
    sharedRolloutStrategies = builder.sharedRolloutStrategies;
  }

  public UUID getId() { return id; }

  @Version
  private long version;

  @ManyToOne
  @Column(name = "fk_who_updated", nullable = true)
  @JoinColumn(name = "fk_who_updated")
  @DbForeignKey(onDelete = ConstraintMode.SET_NULL)
  private DbPerson whoUpdated;

  @Column(length = 400)
  private String whatUpdated;

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

  public void setWhatUpdated(String whatUpdated) {
    this.whatUpdated = whatUpdated;
  }

  public String getWhatUpdated() {
    return whatUpdated;
  }

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
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

  public long getVersion() {
    return version;
  }


  public static final class Builder {
    private DbPerson whoUpdated;
    private String whatUpdated;
    private DbEnvironment environment;
    private DbApplicationFeature feature;
    private FeatureState featureState;
    private String defaultValue;
    private boolean locked;
    private List<RolloutStrategy> rolloutStrategies;
    private List<DbStrategyForFeatureValue> sharedRolloutStrategies;

    public Builder() {
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
