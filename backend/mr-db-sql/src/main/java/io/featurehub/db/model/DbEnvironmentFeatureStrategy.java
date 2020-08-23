package io.featurehub.db.model;

import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.featurehub.mr.model.RolloutStrategyInstance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fh_env_feature_strategy")
public class DbEnvironmentFeatureStrategy {
  @Id
  private UUID id;

  private DbEnvironmentFeatureStrategy(Builder builder) {
    setWhoUpdated(builder.whoUpdated);
    whatUpdated = builder.whatUpdated;
    setEnvironment(builder.environment);
    setFeature(builder.feature);
    setFeatureState(builder.featureState);
    setDefaultValue(builder.defaultValue);
    setLocked(builder.locked);
    setRolloutStrategyInstances(builder.rolloutStrategyInstances);
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

  @DbJson
  @Column(name="rollout_strat")
  private List<RolloutStrategyInstance> rolloutStrategyInstances;

  public DbEnvironmentFeatureStrategy() {
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

  public List<RolloutStrategyInstance> getRolloutStrategyInstances() {
    return rolloutStrategyInstances;
  }

  public void setRolloutStrategyInstances(List<RolloutStrategyInstance> rolloutStrategyInstances) {
    this.rolloutStrategyInstances = rolloutStrategyInstances;
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
    private List<RolloutStrategyInstance> rolloutStrategyInstances;

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

    public Builder rolloutStrategyInstances(List<RolloutStrategyInstance> val) {
      rolloutStrategyInstances = val;
      return this;
    }

    public DbEnvironmentFeatureStrategy build() {
      return new DbEnvironmentFeatureStrategy(this);
    }
  }
}
