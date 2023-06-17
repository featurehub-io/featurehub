package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.Index;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

@Index(unique = true, name = "idx_service_name", columnNames = {"fk_portfolio_id", "name"})
@Entity
@Table(name = "fh_service_account")
@ChangeLog
public class DbServiceAccount extends DbVersionedBase {
  @Column(length = 100)
  private String name;
  @Column(length = 400)
  private String description;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_service_account_id")
  private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_changed_by")
  @Column(name = "fk_changed_by")
  private DbPerson whoChanged;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  private DbPerson whoCreated;

  @OneToOne(optional = true)
  @JoinColumn(name = "fk_sdk_person")
  @Column(name = "fk_sdk_person", nullable = true)
  private DbPerson sdkPerson;

  @Column(name = "api_key", unique = true, nullable = false, length = 100)
  private String apiKeyServerEval;
  @Column(name = "api_key_client_eval", unique = true, nullable = true, length = 100)
  private String apiKeyClientEval;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_portfolio_id")
  @Column(name = "fk_portfolio_id", nullable = false)
  private DbPortfolio portfolio;

  public DbServiceAccount() {
  }

  private DbServiceAccount(Builder builder) {
    setName(builder.name);
    setDescription(builder.description);
    setServiceAccountEnvironments(builder.serviceAccountEnvironments);
    setWhoChanged(builder.whoChanged);
    setWhoCreated(builder.whoChanged);
    setApiKeyServerEval(builder.apiKeyServerEval);
    setApiKeyClientEval(builder.apiKeyClientEval);
    setPortfolio(builder.portfolio);
    setSdkPerson(builder.sdkPerson);
  }

  /**
   * Indicates this environment is removed from the publishing list for some reason, usually because the organisation
   * expired. This is different from archiving because archiving is an action taken by the customer, whereas
   * unpublishing is a system action.
   */
  private Instant whenUnpublished;

  public Instant getWhenUnpublished() {
    return whenUnpublished;
  }

  public void setWhenUnpublished(Instant whenUnpublished) {
    this.whenUnpublished = whenUnpublished;
  }

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  @Nullable public DbPerson getSdkPerson() {
    return sdkPerson;
  }

  public void setSdkPerson(@NotNull DbPerson sdkPerson) {
    this.sdkPerson = sdkPerson;
  }

  public DbPortfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(DbPortfolio portfolio) {
    this.portfolio = portfolio;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<DbServiceAccountEnvironment> getServiceAccountEnvironments() {
    return serviceAccountEnvironments;
  }

  public String getApiKeyServerEval() {
    return apiKeyServerEval;
  }

  public void setApiKeyServerEval(String apiKeyServerEval) {
    this.apiKeyServerEval = apiKeyServerEval;
  }

  public void setServiceAccountEnvironments(Set<DbServiceAccountEnvironment> serviceAccountEnvironments) {
    this.serviceAccountEnvironments = serviceAccountEnvironments;
  }

  public DbPerson getWhoChanged() {
    return whoChanged;
  }

  public void setWhoChanged(DbPerson whoChanged) {
    this.whoChanged = whoChanged;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public String getApiKeyClientEval() {
    return apiKeyClientEval;
  }

  public void setApiKeyClientEval(String apiKeyClientEval) {
    this.apiKeyClientEval = apiKeyClientEval;
  }

  public static final class Builder {
    private String name;
    private String description;
    private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;
    private DbPerson whoChanged;
    private String apiKeyServerEval;
    private String apiKeyClientEval;
    private DbPortfolio portfolio;
    private final DbPerson sdkPerson;

    public Builder(DbPerson sdkPerson) {
      this.sdkPerson = sdkPerson;
    }

    public Builder name(String val) {
      name = val;
      return this;
    }

    public Builder description(String val) {
      description = val;
      return this;
    }

    public Builder serviceAccountEnvironments(Set<DbServiceAccountEnvironment> val) {
      serviceAccountEnvironments = val;
      return this;
    }

    public Builder apiKeyClientEval(String val) {
      apiKeyClientEval = val;
      return this;
    }

    public Builder whoChanged(DbPerson val) {
      whoChanged = val;
      return this;
    }

    public Builder apiKeyServerEval(String val) {
      apiKeyServerEval = val;
      return this;
    }

    public Builder portfolio(DbPortfolio val) {
      portfolio = val;
      return this;
    }

    public DbServiceAccount build() {
      return new DbServiceAccount(this);
    }
  }
}
