package io.featurehub.db.model;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fh_acl")
public class DbAcl {
  @Id
  private UUID id;

  private DbAcl(Builder builder) {
    setEnvironment(builder.environment);
    setApplication(builder.application);
    setGroup(builder.group);
    setRoles(builder.roles);
  }

  private UUID getId() { return id; }

  @Version
  private long version;

  @WhenModified
  @Column(name = "when_updated")
  private LocalDateTime whenUpdated;
  @WhenCreated
  @Column(name = "when_created")
  private LocalDateTime whenCreated;

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

  public void setId(UUID id) {
    this.id = id;
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

  public long getVersion() {
    return version;
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
