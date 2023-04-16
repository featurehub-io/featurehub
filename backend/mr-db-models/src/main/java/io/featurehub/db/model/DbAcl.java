package io.featurehub.db.model;

import io.ebean.annotation.ChangeLog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "fh_acl")
@ChangeLog
public class DbAcl extends DbVersionedBase {
  private DbAcl(Builder builder) {
    setEnvironment(builder.environment);
    setApplication(builder.application);
    setGroup(builder.group);
    setRoles(builder.roles);
  }

  @ManyToOne(optional = true)
  @Column(name = "environment_id")
  @JoinColumn(name = "environment_id")
  private DbEnvironment environment;

  @ManyToOne(optional = true)
  @Column(name = "application_id")
  @JoinColumn(name = "application_id")
  private DbApplication application;

  @ManyToOne
  @Column(name = "group_id")
  private DbGroup group;

  // if it is an application Acl, the roles are ApplicationRoleTypes, otherwise they are RoleTypes
  private String roles;

  public DbAcl() {
  }

  public DbEnvironment getEnvironment() {
    return environment;
  }

  public void setEnvironment(DbEnvironment environment) {
    this.environment = environment;
  }

  public DbGroup getGroup() {
    return group;
  }

  public void setGroup(DbGroup group) {
    this.group = group;
  }

  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }

  public DbApplication getApplication() {
    return application;
  }

  public void setApplication(DbApplication application) {
    this.application = application;
  }

  public static final class Builder {
    private DbEnvironment environment;
    private DbApplication application;
    private DbGroup group;
    private String roles;

    public Builder() {
    }

    public Builder environment(DbEnvironment val) {
      environment = val;
      return this;
    }

    public Builder application(DbApplication val) {
      application = val;
      return this;
    }

    public Builder group(DbGroup val) {
      group = val;
      return this;
    }

    public Builder roles(String val) {
      roles = val;
      return this;
    }

    public DbAcl build() {
      return new DbAcl(this);
    }
  }
}
