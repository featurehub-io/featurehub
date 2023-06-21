package io.featurehub.db.api;

import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RoleType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PersonFeaturePermission {
  public Person person;
  private final Set<RoleType> roles;
  private final Set<ApplicationRoleType> appRoles;

  public PersonFeaturePermission(Person person, Set<RoleType> roles) {
    this.person = person;
    this.roles = roles;
    this.appRoles = new HashSet<>();
  }

  private PersonFeaturePermission(Builder builder) {
    person = builder.person;
    roles = builder.roles;
    appRoles = builder.appRoles;
  }

  public boolean hasNoRoles() {
    return roles.isEmpty() && appRoles.isEmpty();
  }

  public boolean hasWriteRole() {
    return roles.contains(RoleType.CHANGE_VALUE) || roles.contains(RoleType.UNLOCK) || roles.contains(RoleType.LOCK) || hasCreateFeatureRole() || hasEditFeatureRole();
  }

  public boolean hasChangeValueRole() {
    return roles.contains(RoleType.CHANGE_VALUE);

  }

  public boolean hasCreateFeatureRole() {
    return appRoles.contains(ApplicationRoleType.EDIT) || appRoles.contains(ApplicationRoleType.CREATE);
  }

  public boolean hasEditFeatureRole() {
    return appRoles.contains(ApplicationRoleType.EDIT) || appRoles.contains(ApplicationRoleType.EDIT_AND_DELETE);
  }

  public boolean hasLockRole() {
    return roles.contains(RoleType.LOCK) || hasCreateFeatureRole();
  }

  public boolean hasUnlockRole() {
    return roles.contains(RoleType.UNLOCK) || hasCreateFeatureRole();
  }

  @Override
  public String toString() {
    return "PersonFeaturePermission{" +
      "roles=" + roles +
      ", appRoles=" + appRoles +
      '}';
  }

  public static final class Builder {
    private Person person;
    private Set<RoleType> roles = Collections.emptySet();
    private Set<ApplicationRoleType> appRoles = Collections.emptySet();

    public Builder() {
    }

    public Builder person(Person val) {
      person = val;
      return this;
    }

    public Builder roles(Set<RoleType> val) {
      roles = val;
      return this;
    }

    public Builder appRoles(Set<ApplicationRoleType> val) {
      appRoles = val;
      return this;
    }

    public PersonFeaturePermission build() {
      return new PersonFeaturePermission(this);
    }
  }
}
