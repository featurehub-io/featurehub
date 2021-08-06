package io.featurehub.db.model;

import io.ebean.annotation.DbDefault;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fh_service_account_env")
public class DbServiceAccountEnvironment extends DbVersionedBase {
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_environment_id")
  @Column(name = "fk_environment_id")
  private DbEnvironment environment;
  @Column(length = 200)
  private String permissions;
  @ManyToOne(optional = false)
  @JoinColumn(name = "fk_service_account_id")
  @Column(name = "fk_service_account_id")
  private DbServiceAccount serviceAccount;

  @Column
  private Instant whenUsagePeriodStarted;

  @Column
  @DbDefault("0")
  private long usageCounter;

  @OneToMany
  private List<DbApiKeyUsageCounter> usageCounters;

  private DbServiceAccountEnvironment(Builder builder) {
    setId(builder.id);
    setEnvironment(builder.environment);
    setPermissions(builder.permissions);
    setServiceAccount(builder.serviceAccount);
  }

  public DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(DbEnvironment environment) {
    this.environment = environment;
  }

  public String getPermissions() {
    return permissions;
  }

  public void setPermissions(String permissions) {
    this.permissions = permissions;
  }

  public DbServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(DbServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }


  public static final class Builder {
    private UUID id;
    private DbEnvironment environment;
    private String permissions;
    private DbServiceAccount serviceAccount;

    public Builder() {
    }

    public Builder id(UUID val) {
      id = val;
      return this;
    }

    public Builder environment(DbEnvironment val) {
      environment = val;
      return this;
    }

    public Builder permissions(String val) {
      permissions = val;
      return this;
    }

    public Builder serviceAccount(DbServiceAccount val) {
      serviceAccount = val;
      return this;
    }

    public DbServiceAccountEnvironment build() {
      return new DbServiceAccountEnvironment(this);
    }
  }

  public Instant getWhenUsagePeriodStarted() {
    return whenUsagePeriodStarted;
  }

  public void setWhenUsagePeriodStarted(Instant whenUsagePeriodStarted) {
    this.whenUsagePeriodStarted = whenUsagePeriodStarted;
  }

  public long getUsageCounter() {
    return usageCounter;
  }

  public void setUsageCounter(long usageCounter) {
    this.usageCounter = usageCounter;
  }

  public List<DbApiKeyUsageCounter> getUsageCounters() {
    return usageCounters;
  }

  public void setUsageCounters(List<DbApiKeyUsageCounter> usageCounters) {
    this.usageCounters = usageCounters;
  }
}
