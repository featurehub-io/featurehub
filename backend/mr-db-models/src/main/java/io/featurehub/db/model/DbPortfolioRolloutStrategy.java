package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.Index;
import io.featurehub.mr.model.PortfolioRolloutStrategy;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Index(unique = true, name = "idx_pf_strategies", columnNames = {"fk_portfolio_id", "strategy_name"})
@Index(unique = true, name = "idx_pf_strat_code", columnNames = {"fk_portfolio_id", "code"})
@Entity
@Table(name = "fh_port_strategy")
@ChangeLog
public class DbPortfolioRolloutStrategy  extends DbVersionedBase {
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_portfolio_id")
  private final DbPortfolio portfolio;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @Column(name = "strategy_name", nullable = false, length = 150)
  private String name;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_rs_id")
  @Nullable
  private List<DbPortfolioStrategyForFeatureValue> sharedRolloutStrategies;

  // we use the same object ARS for a portfolio level one as they are functionally the same
  @DbJson
  @Column(name = "strategy", nullable = false)
  @NotNull
  private PortfolioRolloutStrategy strategy;

  @NotNull
  @Column(nullable = false, name = "code", length = 10)
  private final String shortUniqueCode;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_changed")
  private DbPerson whoChanged;

  public DbPortfolioRolloutStrategy(@NotNull DbPortfolio portfolio, @NotNull String shortUniqueCode, @NotNull PortfolioRolloutStrategy strategy) {
    this.portfolio = portfolio;
    this.shortUniqueCode = shortUniqueCode;
    this.strategy = strategy;
  }

  public DbPerson getWhoChanged() {
    return whoChanged;
  }

  public void setWhoChanged(DbPerson whoChanged) {
    this.whoChanged = whoChanged;
  }

  public DbPortfolio getPortfolio() {
    return portfolio;
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

  public @Nullable List<DbPortfolioStrategyForFeatureValue> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(@Nullable List<DbPortfolioStrategyForFeatureValue> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }

  public void addSharedRolloutStrategy(DbPortfolioStrategyForFeatureValue strategyLink) {
    if (sharedRolloutStrategies == null) {
      sharedRolloutStrategies = new LinkedList<>();
    }

    sharedRolloutStrategies.add(strategyLink);
  }

  public PortfolioRolloutStrategy getStrategy() {
    return strategy;
  }

  public String getShortUniqueCode() {
    return shortUniqueCode;
  }
}
