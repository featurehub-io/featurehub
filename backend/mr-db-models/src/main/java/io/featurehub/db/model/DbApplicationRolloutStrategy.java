package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.featurehub.mr.model.RolloutStrategyAttribute;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Index(unique = true, name = "idx_app_strategies", columnNames = {"fk_app_id", "strategy_name"})
@Index(unique = true, name = "idx_app_strat_code", columnNames = {"fk_app_id", "code"})
@Entity
@Table(name = "fh_app_strategy")
@ChangeLog
public class DbApplicationRolloutStrategy extends DbVersionedBase {
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_app_id")
  @Column(name = "fk_app_id")
  private final DbApplication application;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @Column(name = "strategy_name", nullable = false, length = 150)
  private String name;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_rs_id")
  @Nullable
  private List<DbStrategyForFeatureValue> sharedRolloutStrategies;

  // we have chosen to do this because we always grab the
  // whole tree, we aren't interested in its constituent parts
  // if we change it in a backwards incompatible fashion
  // we can introduce another database field with the new record
  // and migrate it on the fly
  @DbJson
  @Nullable
  @Column(name = "attr")
  private List<RolloutStrategyAttribute> attributes;

  @NotNull
  @Column(nullable = false, name = "code")
  private final String shortUniqueCode;

  @Column(nullable = false)
  private boolean disabled = false;

  @Column(length = 200)
  @Nullable
  private String avatar;

  @Nullable
  private Integer colouring;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_changed")
  @Column(name = "fk_person_who_changed")
  private DbPerson whoChanged;

  public DbApplicationRolloutStrategy(@NotNull DbApplication application, @NotNull String shortUniqueCode) {
    this.application = application;
    this.shortUniqueCode = shortUniqueCode;
  }

  public DbPerson getWhoChanged() {
    return whoChanged;
  }

  public void setWhoChanged(DbPerson whoChanged) {
    this.whoChanged = whoChanged;
  }

  public DbApplication getApplication() {
    return application;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public @Nullable List<DbStrategyForFeatureValue> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(@Nullable List<DbStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }

  public void addSharedRolloutStrategy(DbStrategyForFeatureValue strategyLink) {
    if (sharedRolloutStrategies == null) {
      sharedRolloutStrategies = new LinkedList<>();
    }

    sharedRolloutStrategies.add(strategyLink);
  }

  public @Nullable List<RolloutStrategyAttribute> getAttributes() {
    return attributes;
  }

  public void setAttributes(@Nullable List<RolloutStrategyAttribute> attributes) {
    this.attributes = attributes;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public @Nullable String getAvatar() {
    return avatar;
  }

  public void setAvatar(@Nullable String avatar) {
    this.avatar = avatar;
  }

  public @Nullable Integer getColouring() {
    return colouring;
  }

  public void setColouring(@Nullable Integer colouring) {
    this.colouring = colouring;
  }

  public String getShortUniqueCode() {
    return shortUniqueCode;
  }
}
