package io.featurehub.db.model;

import io.ebean.annotation.Index;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

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
import java.util.Set;
import java.util.UUID;

@Index(unique = true, name = "idx_service_name", columnNames = {"fk_portfolio_id", "name"})
@Entity
@Table(name = "fh_service_account")
public class DbServiceAccount {
  @Id
  private UUID id;
  @Column(length = 40)
  private String name;
  @Column(length = 400)
  private String description;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_service_account_id")
  private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  private DbPerson whoChanged;
  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;
  @Column(name = "api_key", unique = true, nullable = false, length = 100)
  private String apiKeyServerEval;
  @Column(name = "api_key_client_eval", unique = true, nullable = true, length = 100)
  private String apiKeyClientEval;
  @Version
  private long version;

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
    setApiKeyServerEval(builder.apiKeyServerEval);
    setApiKeyClientEval(builder.apiKeyClientEval);
    portfolio = builder.portfolio;
  }


  public DbPortfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(DbPortfolio portfolio) {
    this.portfolio = portfolio;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
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

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

  public long getVersion() {
    return version;
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


    public Builder() {
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
