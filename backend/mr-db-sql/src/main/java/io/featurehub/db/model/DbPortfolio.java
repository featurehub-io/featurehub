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

@Index(unique = true, name = "idx_portfolio_name", columnNames = {"name", "fk_org_id"})
@Entity
@Table(name = "fh_portfolio")
public class DbPortfolio {

  public DbPortfolio() {}

  @Id
  private UUID id;

  private DbPortfolio(Builder builder) {
    setWhoCreated(builder.whoCreated);
    setOrganization(builder.organization);
    setName(builder.name);
    setDescription(builder.description);
  }

  public UUID getId() { return id; }


  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  private DbPerson whoCreated;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_org_id", referencedColumnName = "id")
  @Column(name = "fk_org_id", nullable = false)
  private DbOrganization organization;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_portfolio_id")
  private Set<DbApplication> applications;
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_portfolio_id")
  private Set<DbGroup> groups;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_portfolio_id")
  private Set<DbServiceAccount> serviceAccounts;

  @Column
  private String name;
  private String description;

  @Version
  private long version;

  public long getVersion() {
    return version;
  }

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  public DbOrganization getOrganization() {
    return organization;
  }

  public void setOrganization(DbOrganization organization) {
    this.organization = organization;
  }

  public Set<DbApplication> getApplications() {
    return applications;
  }

  public void setApplications(Set<DbApplication> applications) {
    this.applications = applications;
  }

  public Set<DbGroup> getGroups() {
    return groups;
  }

  public void setGroups(Set<DbGroup> groups) {
    this.groups = groups;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<DbServiceAccount> getServiceAccounts() {
    return serviceAccounts;
  }

  public void setServiceAccounts(Set<DbServiceAccount> serviceAccounts) {
    this.serviceAccounts = serviceAccounts;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public static final class Builder {
    private DbPerson whoCreated;
    private DbOrganization organization;
    private String name;
    private String description;

    public Builder() {
    }

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public Builder organization(DbOrganization val) {
      organization = val;
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

    public DbPortfolio build() {
      return new DbPortfolio(this);
    }
  }
}
