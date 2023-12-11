package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import io.ebean.annotation.ConstraintMode;
import io.ebean.annotation.DbForeignKey;
import io.ebean.annotation.DbJson;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "fh_environment")
@ChangeLog
public class DbEnvironment extends DbVersionedBase {
  public DbEnvironment() {}

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
    setUserEnvironmentInfo(builder.userEnvironmentInfo);
    setWebhookEnvironmentInfo(builder.webhookEnvironmentInfo);
  }

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
  private Set<DbFeatureValue> environmentFeatures;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "fk_environment_id")
  private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;

  @Column(name = "when_archived")
  private LocalDateTime whenArchived;

  /**
   * Indicates this environment is removed from the publishing list for some reason, usually because
   * the organisation expired. This is different from archiving because archiving is an action taken
   * by the customer, whereas unpublishing is a system action.
   */
  private Instant whenUnpublished;

  @DbJson
  @Column(name = "u_env_inf")
  private Map<String, String> userEnvironmentInfo;

  @DbJson
  @Column(name = "m_env_inf")
  private Map<String, String> managementEnvironmentInfo;

  @DbJson
  @Column(name = "w_env_inf")
  private Map<String, String> webhookEnvironmentInfo;

  public Instant getWhenUnpublished() {
    return whenUnpublished;
  }

  public void setWhenUnpublished(Instant whenUnpublished) {
    this.whenUnpublished = whenUnpublished;
  }

  public DbEnvironment getPriorEnvironment() {
    return priorEnvironment;
  }

  public void setPriorEnvironment(DbEnvironment priorEnvironment) {
    this.priorEnvironment = priorEnvironment;
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

  public Set<DbFeatureValue> getEnvironmentFeatures() {
    return environmentFeatures;
  }

  public void setEnvironmentFeatures(Set<DbFeatureValue> environmentFeatures) {
    this.environmentFeatures = environmentFeatures;
  }

  public Set<DbServiceAccountEnvironment> getServiceAccountEnvironments() {
    return serviceAccountEnvironments;
  }

  public void setServiceAccountEnvironments(
      Set<DbServiceAccountEnvironment> serviceAccountEnvironments) {
    this.serviceAccountEnvironments = serviceAccountEnvironments;
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

  public Map<String, String> getUserEnvironmentInfo() {
    return userEnvironmentInfo;
  }

  public void setUserEnvironmentInfo(Map<String, String> userEnvironmentInfo) {
    this.userEnvironmentInfo = userEnvironmentInfo;
  }

  public Map<String, String> getManagementEnvironmentInfo() {
    return managementEnvironmentInfo;
  }

  public void setManagementEnvironmentInfo(
      @Nullable Map<String, String> managementEnvironmentInfo) {
    this.managementEnvironmentInfo = managementEnvironmentInfo;
  }

  @Nullable
  public Map<String, String> getWebhookEnvironmentInfo() {
    return webhookEnvironmentInfo;
  }

  public void setWebhookEnvironmentInfo(Map<String, String> webhookEnvironmentInfo) {
    this.webhookEnvironmentInfo = webhookEnvironmentInfo;
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
    private Set<DbFeatureValue> environmentFeatures;
    private Set<DbServiceAccountEnvironment> serviceAccountEnvironments;
    private Map<String, String> userEnvironmentInfo;
    @Nullable public Map<String, String> webhookEnvironmentInfo;

    public Builder() {}

    public Builder whoUpdated(DbPerson val) {
      whoUpdated = val;
      return this;
    }

    public Builder whoCreated(DbPerson val) {
      whoCreated = val;
      return this;
    }

    public Builder userEnvironmentInfo(Map<String, String> val) {
      userEnvironmentInfo = val;
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

    public Builder environmentFeatures(Set<DbFeatureValue> val) {
      environmentFeatures = val;
      return this;
    }

    public Builder serviceAccountEnvironments(Set<DbServiceAccountEnvironment> val) {
      serviceAccountEnvironments = val;
      return this;
    }

    public Builder webhookEnvironmentInfo(@Nullable Map<String, String> val) {
      webhookEnvironmentInfo = val;
      return this;
    }

    public DbEnvironment build() {
      return new DbEnvironment(this);
    }
  }
}
