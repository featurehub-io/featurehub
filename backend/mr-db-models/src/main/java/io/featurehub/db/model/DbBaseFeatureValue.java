package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.featurehub.mr.model.RolloutStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.List;

@MappedSuperclass
public class DbBaseFeatureValue extends Model {
  @WhenCreated
  @Column(name = "when_created")
  protected LocalDateTime whenCreated;

  @ManyToOne
  @Column(name = "fk_who_updated", nullable = true)
  @JoinColumn(name = "fk_who_updated")
  @DbForeignKey(onDelete = ConstraintMode.SET_NULL)
  protected DbPerson whoUpdated;

  @Enumerated(value = EnumType.STRING)
  protected FeatureState featureState;

  @Lob
  private String defaultValue;

  @Column(nullable = false)
  protected boolean locked;


  // a user can have multiple strategies here that are specific to this feature value
  // these are usually percentage only ones, but that may change in the future
  @DbJson
  @Column(name="rollout_strat")
  protected List<RolloutStrategy> rolloutStrategies;


  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
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

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

}
