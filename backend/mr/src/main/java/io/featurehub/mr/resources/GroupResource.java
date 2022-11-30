package io.featurehub.mr.resources;

import io.featurehub.db.FilterOptType;
import io.featurehub.db.api.DuplicateUsersException;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.mr.api.GroupServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class GroupResource implements GroupServiceDelegate {
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

  private void groupCheck(UUID gid, Person person, Consumer<Group> action) {
    Group group = groupApi.getGroup(gid, Opts.empty(), person);

    if (group == null) {
      throw new NotFoundException("No such group");
    }

    action.accept(group);
  }

  private Person personCheck(UUID id, Consumer<Person> action) {
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
      throw new ForbiddenException(error);
    }
  }

  private boolean isGroupMember(Person user, Group group) {
    return group.getMembers().stream().map(Person::getId).anyMatch(uid -> uid.equals(user.getId()));
  }


  @Override
  public Group addPersonToGroup(UUID gid, UUID personId, AddPersonToGroupHolder holder,
                                SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, authManager.from(securityContext), group -> {
      personCheck(personId, person -> {
        isAdminOfGroup(group, securityContext, "No permission to add user to group.",  adminGroup -> {
          groupHolder.group = groupApi.addPersonToGroup(gid, personId, new Opts().add(FillOpts.Members, holder.includeMembers));
        });
      });
    });

    if (groupHolder.group == null) {
      throw new NotFoundException();
    }

    return groupHolder.group;
  }

  @Override
  public Group createGroup(UUID id, Group group, CreateGroupHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, current, null)) {
      try {
        return groupApi.createGroup(id, group, current);
      } catch (GroupApi.DuplicateGroupException e) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    }

    throw new ForbiddenException("No permission to add application");
  }

  @Override
  public Boolean deleteGroup(UUID gid, DeleteGroupHolder holder, SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, authManager.from(securityContext), group -> {
      if (group.getAdmin()) {
        throw new ForbiddenException("Cannot delete admin group from deleteGroup method.");
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
  public Group deletePersonFromGroup(UUID gid, UUID personId, DeletePersonFromGroupHolder holder,
                                     SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, authManager.from(securityContext), group -> {
      personCheck(personId, person -> {
        isAdminOfGroup(group, securityContext, "No permission to delete user to group.",
          adminGroup ->
            groupHolder.group = groupApi.deletePersonFromGroup(gid, personId, new Opts().add(FillOpts.Members, holder.includeMembers)));
      });
    });

    if (groupHolder.group == null) {
      throw new NotFoundException();
    }

    return groupHolder.group;
  }

  @Override
  public List<Group> findGroups(UUID id, FindGroupsHolder holder, SecurityContext securityContext) {
    final Person from = authManager.from(securityContext);

    if (authManager.isOrgAdmin(from) || authManager.isPortfolioGroupMember(id, from)) {
      final List<Group> groups = groupApi.findGroups(id, holder.filter, holder.order, new Opts().add(FillOpts.People,
        holder.includePeople));

      if (groups == null) {
        throw new NotFoundException();
      }

      return groups;
    }

    throw new ForbiddenException();
  }

  @Override
  public Group getGroup(UUID gid, GetGroupHolder holder, SecurityContext securityContext) {
    Opts opts = new Opts().add(FillOpts.Acls, holder.includeGroupRoles).add(FilterOptType.Application, holder.byApplicationId);

    if (Boolean.TRUE.equals(holder.includeMembers)) {
      opts.add(FillOpts.People);
      opts.add(FillOpts.Members);
    }

    Group group = groupApi.getGroup(gid, opts, authManager.from(securityContext));

    if (group == null) {
      throw new NotFoundException("No such group");
    }

    return group;
  }

  @Override
  public Group getSuperuserGroup(UUID id, SecurityContext securityContext) {
    Group g = groupApi.getSuperuserGroup(id);

    if (g == null) {
      throw new NotFoundException();
    }

    return g;
  }

  @Override
  public Group updateGroup(UUID gid, Group renameDetails, UpdateGroupHolder holder, SecurityContext securityContext) {
    GroupHolder groupHolder = new GroupHolder();

    groupCheck(gid, authManager.from(securityContext), group -> {
      isAdminOfGroup(group, securityContext, "No permission to rename group.",  adminGroup -> {
        try {
          groupHolder.group = groupApi.updateGroup(gid, renameDetails,
            holder.applicationId, Boolean.TRUE.equals(holder.updateMembers),
            Boolean.TRUE.equals(holder.updateApplicationGroupRoles),
            Boolean.TRUE.equals(holder.updateEnvironmentGroupRoles),
            new Opts().add(FillOpts.Members, holder.includeMembers).add(FillOpts.Acls, holder.includeGroupRoles));
        } catch (OptimisticLockingException e) {
          throw new WebApplicationException(422);
        } catch (GroupApi.DuplicateGroupException | DuplicateUsersException e) {
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
