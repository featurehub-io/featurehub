package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "fh_application")
@ChangeLog
public class DbApplication extends DbVersionedBase {
  @Column(length = 100, nullable = false)
  private String name;
  @Column(length = 400)
  private String description;

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

  @DbForeignKey(onDelete = ConstraintMode.CASCADE)
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_app_id")
  private List<DbRolloutStrategy> sharedRolloutStrategies;

  public DbApplication() {}

  private DbApplication(Builder builder) {
    setName(builder.name);
    setDescription(builder.description);
    setWhoCreated(builder.whoCreated);
    setPortfolio(builder.portfolio);
    setEnvironments(builder.environments);
    setFeatures(builder.features);
    setGroupRolesAcl(builder.groupRolesAcl);
    setSharedRolloutStrategies(builder.sharedRolloutStrategies);
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

  public List<DbRolloutStrategy> getSharedRolloutStrategies() {
    return sharedRolloutStrategies;
  }

  public void setSharedRolloutStrategies(List<DbRolloutStrategy> sharedRolloutStrategies) {
    this.sharedRolloutStrategies = sharedRolloutStrategies;
  }


  public static final class Builder {
    private String name;
    private String description;
    private DbPerson whoCreated;
    private DbPortfolio portfolio;
    private Set<DbEnvironment> environments;
    private Set<DbApplicationFeature> features;
    private Set<DbAcl> groupRolesAcl;
    private List<DbRolloutStrategy> sharedRolloutStrategies;

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

    public Builder sharedRolloutStrategies(List<DbRolloutStrategy> val) {
      sharedRolloutStrategies = val;
      return this;
    }

    public DbApplication build() {
      return new DbApplication(this);
    }
  }
}
