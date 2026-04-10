package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Index(unique = true, name = "idx_feature_filter_name", columnNames = {"fk_portfolio_id", "name"})
@Entity
@Table(name = "fh_feature_filter")
@ChangeLog
public class DbFeatureFilter extends DbVersionedBase {

  private DbFeatureFilter(Builder builder) {
    setPortfolio(builder.portfolio);
    setWhoCreated(builder.whoCreated);
    setName(builder.name);
    setDescription(builder.description);
  }

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_portfolio_id")
  @Column(name = "fk_portfolio_id", nullable = false)
  @NotNull
  private DbPortfolio portfolio;

  @ManyToOne(optional = true)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  @Nullable
  private DbPerson whoCreated;

  @Column(name = "name", length = 60, nullable = false)
  @NotNull
  private String name;

  @Column(name = "description", length = 300)
  @Nullable
  private String description;

  // Inverse side: needed so QDbFeatureFilter.serviceAccounts.id path is available for ID-only queries
  @ManyToMany(mappedBy = "featureFilters")
  private List<DbServiceAccount> serviceAccounts;

  // Inverse side: needed so QDbFeatureFilter.applicationFeatures.id path is available for ID-only queries
  @ManyToMany(mappedBy = "filters")
  private List<DbApplicationFeature> applicationFeatures;

  @NotNull
  public DbPortfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(@NotNull DbPortfolio portfolio) {
    this.portfolio = portfolio;
  }

  @Nullable
  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(@Nullable DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  @NotNull
  public String getName() {
    return name;
  }

  public void setName(@NotNull String name) {
    this.name = name;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public List<DbServiceAccount> getServiceAccounts() {
    return serviceAccounts;
  }

  public void setServiceAccounts(List<DbServiceAccount> serviceAccounts) {
    this.serviceAccounts = serviceAccounts;
  }

  public List<DbApplicationFeature> getApplicationFeatures() {
    return applicationFeatures;
  }

  public void setApplicationFeatures(List<DbApplicationFeature> applicationFeatures) {
    this.applicationFeatures = applicationFeatures;
  }

  public static final class Builder {
    private DbPortfolio portfolio;
    private DbPerson whoCreated;
    private String name;
    private String description;

    public Builder() {}

    public Builder portfolio(DbPortfolio val) {
      portfolio = val;
      return this;
    }

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder description(String val) {
      description = val;
      return this;
    }

    public DbFeatureFilter build() {
      return new DbFeatureFilter(this);
    }
  }
}
