package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.featurehub.mr.model.RolloutStrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Index(unique = true, name = "idx_app_strategies", columnNames = {"fk_app_id", "strategy_name"})
@Entity
@Table(name = "fh_app_strategy")
@ChangeLog
public class DbRolloutStrategy extends DbVersionedBase {
  private DbRolloutStrategy(Builder builder) {
    setApplication(builder.application);
    setName(builder.name);
    setStrategy(builder.strategy);
    setWhoChanged(builder.whoChanged);
  }

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

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_changed")
  @Column(name = "fk_person_who_changed")
  private DbPerson whoChanged;

  public DbPerson getWhoChanged() {
    return whoChanged;
  }

  public void setWhoChanged(DbPerson whoChanged) {
    this.whoChanged = whoChanged;
  }

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

  public RolloutStrategy getStrategy() {
    return strategy;
  }

  public void setStrategy(RolloutStrategy strategy) {
    this.strategy = strategy;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<DbStrategyForFeatureValue> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }


  public static final class Builder {
    private DbApplication application;
    private String name;
    private RolloutStrategy strategy;
    private DbPerson whoChanged;

    public Builder() {
    }

    public Builder application(DbApplication val) {
      application = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder strategy(RolloutStrategy val) {
      strategy = val;
      return this;
    }

    public Builder whoChanged(DbPerson val) {
      whoChanged = val;
      return this;
    }

    public DbRolloutStrategy build() {
      return new DbRolloutStrategy(this);
    }
  }
}
