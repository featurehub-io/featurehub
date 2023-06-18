package io.featurehub.db.model;

import io.ebean.annotation.DbJson;
import io.featurehub.mr.model.FeatureGroupStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "fh_featgroup")
public class DbFeatureGroup  extends DbVersionedBase {
  @Column(name = "gp_order")
  private int order;
  @Column(nullable = false)
  @NotNull
  private String name;
  @Column(name = "dscr")
  @Nullable
  private String description;

  @ManyToOne(optional = false)
  @Column(name = "fk_environment_id", nullable = false)
  @JoinColumn(name = "fk_environment_id")
  @NotNull
  private DbEnvironment environment;

  @OneToMany
  @NotNull
  private List<DbFeatureGroupFeature> features;

  public DbFeatureGroup(String name, DbEnvironment environment) {
    this.name = name;
    this.environment = environment;
  }

  @DbJson
  @Lob
  @Nullable
  private FeatureGroupStrategy strategy;

  @Column(nullable = true)
  @Nullable
  private Instant whenArchived;

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public @NotNull String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  public @NotNull DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(@NotNull DbEnvironment environment) {
    this.environment = environment;
  }

  public @NotNull List<DbFeatureGroupFeature> getFeatures() {
    return features;
  }

  public void setFeatures(@NotNull List<DbFeatureGroupFeature> features) {
    this.features = features;
  }

  public @Nullable FeatureGroupStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(@Nullable FeatureGroupStrategy strategy) {
    this.strategy = strategy;
  }

  private DbPerson whoUpdated;

  @Column(nullable = false)
  private DbPerson whoCreated;

  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
  }

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  public @Nullable Instant getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(@Nullable Instant whenArchived) {
    this.whenArchived = whenArchived;
  }

  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }
}
