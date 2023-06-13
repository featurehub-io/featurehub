package io.featurehub.db.model;

import io.ebean.annotation.DbJson;
import io.featurehub.mr.model.FeatureGroupStrategy;
import io.featurehub.mr.model.RolloutStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "fh_featgroup")
public class DbFeatureGroup  extends DbVersionedBase {
  @Column(name = "gp_order")
  private int order;
  private String name;
  @Column(name = "dscr")
  private String description;

  @ManyToOne(optional = false)
  @Column(name = "fk_environment_id", nullable = false)
  @JoinColumn(name = "fk_environment_id")
  private DbEnvironment environment;

  @OneToMany
  private List<DbFeatureGroupFeature> features;

  public DbFeatureGroup(String name, DbEnvironment environment) {
    this.name = name;
    this.environment = environment;
  }

  @DbJson
  @Lob
  private FeatureGroupStrategy strategy;

  @Column(nullable = true)
  private Instant whenArchived;

  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(DbEnvironment environment) {
    this.environment = environment;
  }

  public List<DbFeatureGroupFeature> getFeatures() {
    return features;
  }

  public void setFeatures(List<DbFeatureGroupFeature> features) {
    this.features = features;
  }

  public FeatureGroupStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(FeatureGroupStrategy strategy) {
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

  public Instant getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(Instant whenArchived) {
    this.whenArchived = whenArchived;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
