package io.featurehub.mr.rest;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.mr.api.GroupSecuredService;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.function.Consumer;

@Singleton
public class GroupResource implements GroupSecuredService {
  private final PersonApi personApi;
  private final GroupApi groupApi;
  private final AuthManagerService authManager;

  @Inject
  public GroupResource(PersonApi personApi, GroupApi groupApi, AuthManagerService authManager) {
    this.personApi = personApi;
    this.groupApi = groupApi;
    this.authManager = authManager;
  }

  class GroupHolder {
    Group group;
    boolean delete = false;
  }

  @Override
  public Group addPersonToGroup(String gid, String personId, Boolean includeMembers, SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, group -> {
      personCheck(personId, person -> {
        isAdminOfGroup(group, securityContext, "No permission to add user to group.",  adminGroup -> {
          groupHolder.group = groupApi.addPersonToGroup(gid, personId, new Opts().add(FillOpts.Members, includeMembers));
        });
      });
    });

    if (groupHolder.group == null) {
      throw new NotFoundException();
    }

    return groupHolder.group;
  }

  private void groupCheck(String gid, Consumer<Group> action) {
    Group group = groupApi.getGroup(gid, Opts.empty());

    if (group == null) {
      throw new NotFoundException("No such group");
    }

    action.accept(group);
  }

  private Person personCheck(String id, Consumer<Person> action) {
    Person person = personApi.get(id, Opts.empty());

    if (person == null) {
      throw new NotFoundException("No such person");
    }

    action.accept(person);

    return person;
  }

  private void isAdminOfGroup(Group groupToCheck, SecurityContext securityContext, String error, Consumer<Group> action) {
    Person currentUser = authManager.from(securityContext);
    Group adminGroup = null;

    boolean member = false;

    if (groupToCheck.getPortfolioId() != null) { // this is a portfolio groupToCheck, so find the groupToCheck belonging to this portfolio
      adminGroup = groupApi.findPortfolioAdminGroup(groupToCheck.getPortfolioId(), Opts.opts(FillOpts.Members));
      member = isGroupMember(currentUser, adminGroup);
    }

    if (!member) {
      adminGroup = groupApi.findOrganizationAdminGroup(groupToCheck.getOrganizationId(), Opts.opts(FillOpts.Members));
      member = isGroupMember(currentUser, adminGroup);
    }

    if (member) {
      action.accept(adminGroup);
    } else {
      throw new NotAuthorizedException(error);
    }
  }

  private boolean isGroupMember(Person user, Group group) {
    return group.getMembers().stream().map(Person::getId).anyMatch(uid -> uid.equals(user.getId()));
  }



  @Override
  public Boolean deleteGroup(String gid, Boolean includeMembers, Boolean includeGroupRoles, SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, group -> {
      if (group.getAdmin()) {
        throw new NotAuthorizedException("Cannot delete admin group from deleteGroup method.");
      }

      isAdminOfGroup(group, securityContext, "Not owner of group, cannot delete", adminGroup -> {
        groupApi.deleteGroup(gid);
        groupHolder.delete = true;
      });
    });

    // revisit what we return
    return groupHolder.delete;
  }

  @Override
  public Group deletePersonFromGroup(String gid, String personId, Boolean includeMembers, SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, group -> {
      personCheck(personId, person -> {
        isAdminOfGroup(group, securityContext, "No permission to delete user to group.",
          adminGroup ->
            groupHolder.group = groupApi.deletePersonFromGroup(gid, personId, new Opts().add(FillOpts.Members, includeMembers)));
      });
    });

    if (groupHolder.group == null) {
      throw new NotFoundException();
    }

    return groupHolder.group;
  }

  @Override
  public Group getGroup(String gid, Boolean includeMembers, Boolean includeGroupRoles, SecurityContext sc) {
    Opts opts = new Opts().add(FillOpts.Acls, includeGroupRoles);

    if (Boolean.TRUE.equals(includeMembers)) {
      opts.add(FillOpts.People);
      opts.add(FillOpts.Members);
    }

    Group group = groupApi.getGroup(gid, opts);

    if (group == null) {
      throw new NotFoundException("No such group");
    }

    return group;
  }

  @Override
  public Group updateGroup(String gid, Group renameDetails, Boolean includeMembers, Boolean includeEnvironmentGroupRoles, Boolean updateMembers, Boolean updateEnvironmentGroupRoles, Boolean updateApplicationGroupRoles, SecurityContext sc) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, group -> {
        isAdminOfGroup(group, sc, "No permission to rename group.",  adminGroup -> {
          try {
            groupHolder.group = groupApi.updateGroup(gid, renameDetails,
              Boolean.TRUE.equals(updateMembers),
              Boolean.TRUE.equals(updateApplicationGroupRoles),
              Boolean.TRUE.equals(updateEnvironmentGroupRoles),
              new Opts().add(FillOpts.Members, includeMembers).add(FillOpts.Acls, includeEnvironmentGroupRoles));
          } catch (OptimisticLockingException e) {
            throw new WebApplicationException(422);
          } catch (GroupApi.DuplicateGroupException e) {
            throw new WebApplicationException(Response.Status.CONFLICT);
          }
        });
    });

    if (groupHolder.group == null) {
      throw new NotFoundException();
    }

    return groupHolder.group;
  }
}
