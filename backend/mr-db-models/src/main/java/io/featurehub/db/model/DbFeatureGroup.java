package io.featurehub.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "fh_featgroup")
public class DbFeatureGroup  extends DbVersionedBase {
  @Column(name = "gp_order")
  private int order;
  private String name;

  @ManyToOne(optional = false)
  @Column(name = "fk_environment_id", nullable = false)
  @JoinColumn(name = "fk_environment_id")
  private DbEnvironment environment;

  private List<DbFeatureGroupFeature> features;

  public DbFeatureGroup(String name, DbEnvironment environment) {
    this.name = name;
    this.environment = environment;
  }

  @Lob
  private String strategies;

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

  public String getStrategies() {
    return strategies;
  }

  public void setStrategies(String strategies) {
    this.strategies = strategies;
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
}
