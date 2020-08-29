package io.featurehub.db.model;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.ebean.annotation.WhenCreated;
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

@Index(unique = true, name = "idx_app_strategies", columnNames = {"fk_app_id", "strategy_name"})
@Entity
@Table(name = "fh_app_strategy")
public class DbRolloutStrategy {
  @Id
  private UUID id;

  @Version
  private long version;

  public UUID getId() {
    return id;
  }

  @WhenModified
  @Column(name = "when_updated")
  public LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  public LocalDateTime whenCreated;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_app_id")
  @Column(name = "fk_app_id")
  private DbApplication application;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @Column(name = "strategy_name", nullable = false)
  private String name;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_rs_id")
  private List<DbStrategyForFeatureValue> sharedRolloutStrategies;

  // we have chosen to do this because we always grab the
  // whole tree, we aren't interested in its constituent parts
  // if we change it in a backwards incompatible fashion
  // we can introduce another database field with the new record
  // and migrate it on the fly
  @DbJson
  @Column(nullable = false)
  private RolloutStrategy strategy;

  public DbApplication getApplication() {
    return application;
  }

  public void setApplication(DbApplication application) {
    this.application = application;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

  public List<DbStrategyForFeatureValue> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }
}
