package io.featurehub.db.api;

import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.RoleType;

import java.util.Set;

public class EnvironmentRoles {
  public Set<RoleType> environmentRoles;
  public Set<ApplicationRoleType> applicationRoles;

  private EnvironmentRoles(Builder builder) {
    environmentRoles = builder.environmentRoles;
    applicationRoles = builder.applicationRoles;
  }

  public static final class Builder {
    private Set<RoleType> environmentRoles;
    private Set<ApplicationRoleType> applicationRoles;

    public Builder() {
    }

    public Builder environmentRoles(Set<RoleType> val) {
      environmentRoles = val;
      return this;
    }

    public Builder applicationRoles(Set<ApplicationRoleType> val) {
      applicationRoles = val;
      return this;
    }

    public EnvironmentRoles build() {
      return new EnvironmentRoles(this);
    }
  }
}
