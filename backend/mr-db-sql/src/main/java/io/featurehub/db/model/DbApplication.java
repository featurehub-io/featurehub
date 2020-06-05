package io.featurehub.db.model;

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

@Entity
@Table(name = "fh_application")
public class DbApplication {
  @Id
  private UUID id;

  @Column(length = 100, nullable = false)
  private String name;
  @Column(length = 400)
  private String description;

  @Version
  private long version;

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_person_who_created")
  @Column(name = "fk_person_who_created")
  private DbPerson whoCreated;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_portfolio_id")
  @Column(name = "fk_portfolio_id")
  private DbPortfolio portfolio;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_app_id")
  private Set<DbEnvironment> environments;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_app_id")
  private Set<DbApplicationFeature> features;

  // this is a list of application roles
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "application_id")
  private Set<DbAcl> groupRolesAcl;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  public DbApplication() {}

  private DbApplication(Builder builder) {
    setName(builder.name);
    setDescription(builder.description);
    setWhoCreated(builder.whoCreated);
    setPortfolio(builder.portfolio);
    setEnvironments(builder.environments);
    setFeatures(builder.features);
    setGroupRolesAcl(builder.groupRolesAcl);
  }

  public UUID getId() { return id; }

  public long getVersion() {
    return version;
  }

  public LocalDateTime getWhenUpdated() {
    return whenUpdated;
  }

  public LocalDateTime getWhenCreated() {
    return whenCreated;
  }

  public DbPortfolio getPortfolio() {
    return portfolio;
  }

  public void setPortfolio(DbPortfolio portfolio) {
    this.portfolio = portfolio;
  }

  public Set<DbEnvironment> getEnvironments() {
    return environments;
  }

  public void setEnvironments(Set<DbEnvironment> environments) {
    this.environments = environments;
  }

  public Set<DbApplicationFeature> getFeatures() {
    return features;
  }

  public void setFeatures(Set<DbApplicationFeature> features) {
    this.features = features;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<DbAcl> getGroupRolesAcl() {
    return groupRolesAcl;
  }

  public void setGroupRolesAcl(Set<DbAcl> groupRolesAcl) {
    this.groupRolesAcl = groupRolesAcl;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public static final class Builder {
    private String name;
    private String description;
    private DbPerson whoCreated;
    private DbPortfolio portfolio;
    private Set<DbEnvironment> environments;
    private Set<DbApplicationFeature> features;
    private Set<DbAcl> groupRolesAcl;

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

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public Builder portfolio(DbPortfolio val) {
      portfolio = val;
      return this;
    }

    public Builder environments(Set<DbEnvironment> val) {
      environments = val;
      return this;
    }

    public Builder features(Set<DbApplicationFeature> val) {
      features = val;
      return this;
    }

    public Builder groupRolesAcl(Set<DbAcl> val) {
      groupRolesAcl = val;
      return this;
    }

    public DbApplication build() {
      return new DbApplication(this);
    }
  }
}
