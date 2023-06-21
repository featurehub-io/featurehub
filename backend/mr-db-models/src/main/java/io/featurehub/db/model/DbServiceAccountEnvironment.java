package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "fh_service_account_env")
@ChangeLog
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
}
