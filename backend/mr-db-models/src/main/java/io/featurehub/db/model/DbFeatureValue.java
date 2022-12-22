package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import io.ebean.annotation.WhenModified;
import io.featurehub.mr.model.RolloutStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fh_env_feature_strategy")
@Index(name = "idx_fv_unique", unique = true, columnNames = {"fk_environment_id", "fk_feature_id"})
@ChangeLog
public class DbFeatureValue extends DbBaseFeatureValue {
  private DbFeatureValue(Builder builder) {
    setWhoUpdated(builder.whoUpdated);
    setEnvironment(builder.environment);
    setFeature(builder.feature);
    setFeatureState(builder.featureState);
    setDefaultValue(builder.defaultValue);
    setLocked(builder.locked);
    setRolloutStrategies(builder.rolloutStrategies);
    setRetired(builder.retired);
    setSharedRolloutStrategies(builder.sharedRolloutStrategies);
    setVersion(builder.version);
  }

  @Id
  private UUID id;

  @Version
  private Long version;

  /**
   * historically this field didn't exist, so we can't force it to non-null.
   * When null or false, it is not retired,
   * when
   * true it is retired and Edge won't
   * see it.
   */
  @Column
  protected Boolean retired;

  @WhenModified
  @Column(name = "when_updated")
  protected LocalDateTime whenUpdated;

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public Boolean getRetired() {
    return retired;
  }

  public void setRetired(Boolean retired) {
    this.retired = retired;
  }

  public Long getVersion() {
    return version;
  }

  public UUID getId() { return id; }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setVersion(Long version) {
    this.version = version;
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

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_fv_id")
  protected List<DbStrategyForFeatureValue> sharedRolloutStrategies;

  public List<DbStrategyForFeatureValue> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }


  public DbFeatureValue() {
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

  public static final class Builder {
    private DbPerson whoUpdated;
    private DbEnvironment environment;
    private DbApplicationFeature feature;
    private FeatureState featureState;
    private String defaultValue;
    private boolean locked;
    private Boolean retired; // null == true
    private List<RolloutStrategy> rolloutStrategies;
    private List<DbStrategyForFeatureValue> sharedRolloutStrategies;
    private Long version;

    public Builder() {
    }

    public Builder version(Long version) {
      this.version = version;
      return this;
    }

    public Builder retired(Boolean retired) {
      this.retired = retired;
      return this;
    }

    public Builder whoUpdated(DbPerson val) {
      whoUpdated = val;
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
