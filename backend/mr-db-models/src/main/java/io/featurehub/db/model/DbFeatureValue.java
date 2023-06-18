package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import io.ebean.annotation.WhenModified;
import io.featurehub.mr.model.RolloutStrategy;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fh_env_feature_strategy")
@Index(name = "idx_fv_unique", unique = true, columnNames = {"fk_environment_id", "fk_feature_id"})
@ChangeLog
public class DbFeatureValue extends DbBaseFeatureValue {
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
  protected boolean retired;

  @WhenModified
  @Column(name = "when_updated")
  protected LocalDateTime whenUpdated;

  // in sql, create a unique index on these two
  @ManyToOne(optional = false)
  @Column(name = "fk_environment_id", nullable = false)
  @JoinColumn(name = "fk_environment_id")
  @NotNull
  private DbEnvironment environment;

  @ManyToOne(optional = false)
  @Column(name = "fk_feature_id")
  @JoinColumn(name = "fk_feature_id")
  @NotNull
  private DbApplicationFeature feature;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_fv_id")
  @Nullable
  protected List<DbStrategyForFeatureValue> sharedRolloutStrategies;

  public DbFeatureValue(@NotNull DbPerson whoUpdated, boolean locked, @NotNull DbApplicationFeature feature,
                        @NotNull DbEnvironment environment,
                        @Nullable String defaultValue) {
    super(whoUpdated, locked);

    this.feature = feature;
    this.environment = environment;
    setDefaultValue(defaultValue);

  }

  @NotNull
  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public boolean getRetired() {
    return retired;
  }

  public void setRetired(boolean retired) {
    this.retired = retired;
  }

  public Long getVersion() {
    return version;
  }

  public UUID getId() { return id; }

  public void setId(@NotNull UUID id) {
    this.id = id;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public @Nullable List<DbStrategyForFeatureValue> getSharedRolloutStrategies() {
    if (sharedRolloutStrategies == null) {
      sharedRolloutStrategies = new LinkedList<>();
    }
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(@Nullable List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }

  public @NotNull DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(@NotNull DbEnvironment environment) {
    this.environment = environment;
  }

  public @NotNull DbApplicationFeature getFeature() {
    return feature;
  }

  public void setFeature(@NotNull DbApplicationFeature feature) {
    this.feature = feature;
  }
}
