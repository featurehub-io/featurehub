package io.featurehub.db.model;

import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
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
@Table(name = "fh_environment")
public class DbEnvironment {
  @Id
  private UUID id;

  public DbEnvironment() {
  }

  private DbEnvironment(Builder builder) {
    setWhoUpdated(builder.whoUpdated);
    setWhoCreated(builder.whoCreated);
    setProductionEnvironment(builder.productionEnvironment);
    setPriorEnvironment(builder.priorEnvironment);
    setParentApplication(builder.parentApplication);
    setName(builder.name);
    setDescription(builder.description);
    setGroupRolesAcl(builder.groupRolesAcl);
    setEnvironmentFeatures(builder.environmentFeatures);
    setServiceAccountEnvironments(builder.serviceAccountEnvironments);
  }


  public UUID getId() { return id; }

  @Version
  private long version;

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

  private DbPerson whoUpdated;

  @Column(nullable = false)
  private DbPerson whoCreated;

  @Column(nullable = false, name = "is_prod_environment")
  private boolean productionEnvironment;

  @ManyToOne(optional = true)
  @JoinColumn(name = "fk_prior_env_id")
  @Column(name = "fk_prior_env_id")
  @DbForeignKey(onDelete = ConstraintMode.SET_NULL)
  private DbEnvironment priorEnvironment;

  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_app_id")
  @Column(name = "fk_app_id", nullable = false)
  private DbApplication parentApplication;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 400)
  private String description;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "environment_id")
  private Set<DbAcl> groupRolesAcl;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_environment_id")
  private Set<DbEnvironmentFeatureStrategy> environmentFeatures;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_environment_id")
  private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;


  public DbEnvironment getPriorEnvironment() {
    return priorEnvironment;
  }

  public void setPriorEnvironment(DbEnvironment priorEnvironment) {
    this.priorEnvironment = priorEnvironment;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public long getVersion() {
    return version;
  }

  public DbPerson getWhoUpdated() {
    return whoUpdated;
  }

  public void setWhoUpdated(DbPerson whoUpdated) {
    this.whoUpdated = whoUpdated;
  }

  public DbPerson getWhoCreated() {
    return whoCreated;
  }

  public void setWhoCreated(DbPerson whoCreated) {
    this.whoCreated = whoCreated;
  }

  public DbApplication getParentApplication() {
    return parentApplication;
  }

  public void setParentApplication(DbApplication parentApplication) {
    this.parentApplication = parentApplication;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<DbAcl> getGroupRolesAcl() {
    return groupRolesAcl;
  }

  public void setGroupRolesAcl(Set<DbAcl> groupRolesAcl) {
    this.groupRolesAcl = groupRolesAcl;
  }

  public Set<DbEnvironmentFeatureStrategy> getEnvironmentFeatures() {
    return environmentFeatures;
  }

  public void setEnvironmentFeatures(Set<DbEnvironmentFeatureStrategy> environmentFeatures) {
    this.environmentFeatures = environmentFeatures;
  }

  public Set<DbServiceAccountEnvironment> getServiceAccountEnvironments() {
    return serviceAccountEnvironments;
  }

  public void setServiceAccountEnvironments(Set<DbServiceAccountEnvironment> serviceAccountEnvironments) {
    this.serviceAccountEnvironments = serviceAccountEnvironments;
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

  public boolean isProductionEnvironment() {
    return productionEnvironment;
  }

  public void setProductionEnvironment(boolean productionEnvironment) {
    this.productionEnvironment = productionEnvironment;
  }

  public LocalDateTime getWhenArchived() {
    return whenArchived;
  }

  public void setWhenArchived(LocalDateTime whenArchived) {
    this.whenArchived = whenArchived;
  }

  public static final class Builder {
    private DbPerson whoUpdated;
    private DbPerson whoCreated;
    private boolean productionEnvironment;
    private DbEnvironment priorEnvironment;
    private DbApplication parentApplication;
    private String name;
    private String description;
    private Set<DbAcl> groupRolesAcl;
    private Set<DbEnvironmentFeatureStrategy> environmentFeatures;
    private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;

    public Builder() {
    }

    public Builder whoUpdated(DbPerson val) {
      whoUpdated = val;
      return this;
    }

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public Builder productionEnvironment(boolean val) {
      productionEnvironment = val;
      return this;
    }

    public Builder priorEnvironment(DbEnvironment val) {
      priorEnvironment = val;
      return this;
    }

    public Builder parentApplication(DbApplication val) {
      parentApplication = val;
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

    public Builder groupRolesAcl(Set<DbAcl> val) {
      groupRolesAcl = val;
      return this;
    }

    public Builder environmentFeatures(Set<DbEnvironmentFeatureStrategy> val) {
      environmentFeatures = val;
      return this;
    }

    public Builder serviceAccountEnvironments(Set<DbServiceAccountEnvironment> val) {
      serviceAccountEnvironments = val;
      return this;
    }

    public DbEnvironment build() {
      return new DbEnvironment(this);
    }
  }
}
