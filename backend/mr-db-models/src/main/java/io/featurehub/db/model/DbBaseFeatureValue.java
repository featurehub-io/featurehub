package io.featurehub.db.model;

import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import io.featurehub.mr.model.RolloutStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@MappedSuperclass
public class DbBaseFeatureValue extends Model {
  @WhenCreated
  @Column(name = "when_created")
  protected LocalDateTime whenCreated;

  @ManyToOne
  @Column(name = "fk_who_updated", nullable = false)
  @JoinColumn(name = "fk_who_updated")
  @NotNull
  protected DbPerson whoUpdated;

  @Lob
  @Nullable
  private String defaultValue;

  @Column(nullable = false)
  protected boolean locked;

  // a user can have multiple strategies here that are specific to this feature value
  // these are usually percentage only ones, but that may change in the future
  @DbJson
  @Column(name = "rollout_strat")
  protected List<RolloutStrategy> rolloutStrategies;

  @NotNull
  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(@NotNull DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
  }

  public boolean isLocked() {
    return locked;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  @Nullable
  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(@Nullable
                              String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public @NotNull List<RolloutStrategy> getRolloutStrategies() {
    if (rolloutStrategies == null) {
      rolloutStrategies = new LinkedList<>();
    }

    return rolloutStrategies ;
  }

  public void setRolloutStrategies(@NotNull List<RolloutStrategy> rolloutStrategies) {
    this.rolloutStrategies = rolloutStrategies;
  }

  @NotNull public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

  public DbBaseFeatureValue(@NotNull DbPerson whoUpdated, boolean locked) {
    this.whoUpdated = whoUpdated;
    this.locked = locked;
  }
}
