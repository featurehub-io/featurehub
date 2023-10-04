package io.featurehub.mr.resources

import io.featurehub.db.FilterOptType
import io.featurehub.db.api.*
import io.featurehub.mr.api.GroupServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonId
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import java.util.*
import java.util.function.Consumer

class GroupResource @Inject constructor(
  private val personApi: PersonApi,
  private val groupApi: GroupApi,
  private val authManager: AuthManagerService
) : GroupServiceDelegate {
  internal inner class GroupHolder {
    var group: Group? = null
    var delete = false
  }

  private fun groupCheck(gid: UUID, person: Person, action: Consumer<Group>) {
    val group = groupApi.getGroup(gid, Opts.empty(), person) ?: throw NotFoundException("No such group")
    action.accept(group)
  }

  private fun personCheck(id: UUID, action: Consumer<Person>): Person {
    val person = personApi[id, Opts.empty()] ?: throw NotFoundException("No such person")
    action.accept(person)
    return person
  }

  private fun isAdminOfGroup(
    groupToCheck: Group,
    securityContext: SecurityContext,
    error: String,
    action: Consumer<Group?>
  ) {
    val currentUser = authManager.from(securityContext)
    var adminGroup: Group? = null
    var member = false
    groupToCheck.portfolioId?.let { pId ->
      adminGroup = groupApi.findPortfolioAdminGroup(pId, Opts.opts(FillOpts.Members))
      member = isGroupMember(currentUser, adminGroup)
    }
    if (!member) {
      adminGroup = groupApi.findOrganizationAdminGroup(groupToCheck.organizationId!!, Opts.opts(FillOpts.Members))
      member = isGroupMember(currentUser, adminGroup)
    }
    if (member) {
      action.accept(adminGroup)
    } else {
      throw ForbiddenException(error)
    }
  }

  private fun isGroupMember(user: Person, group: Group?): Boolean {
    return group!!.members
      .stream().map { obj: Person -> obj.id }.anyMatch { uid: PersonId? -> uid == user.id }
  }

  override fun addPersonToGroup(
    gid: UUID, personId: UUID, holder: GroupServiceDelegate.AddPersonToGroupHolder,
    securityContext: SecurityContext
  ): Group {
    val groupHolder = GroupHolder()
    groupCheck(
      gid,
      authManager.from(securityContext)
    ) { group: Group ->
      personCheck(personId) {
        isAdminOfGroup(
          group,
          securityContext,
          "No permission to add user to group."
        ) {
          groupHolder.group =
            groupApi.addPersonToGroup(gid, personId, Opts().add(FillOpts.Members, holder.includeMembers))
        }
      }
    }
    if (groupHolder.group == null) {
      throw NotFoundException()
    }
    return groupHolder.group!!
  }

  override fun createGroup(
    id: UUID,
    createGroup: CreateGroup,
    holder: GroupServiceDelegate.CreateGroupHolder,
    securityContext: SecurityContext?
  ): Group {
    val current = authManager.from(securityContext)
    if (authManager.isPortfolioAdmin(id, current, null)) {
      return try {
        groupApi.createGroup(id, createGroup, current) ?: throw NotFoundException()
      } catch (e: GroupApi.DuplicateGroupException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      }
    }
    throw ForbiddenException("No permission to add application")
  }

  override fun deleteGroup(
    gid: UUID,
    holder: GroupServiceDelegate.DeleteGroupHolder,
    securityContext: SecurityContext
  ): Boolean {
    val groupHolder = GroupHolder()
    groupCheck(gid, authManager.from(securityContext)) { group: Group ->
      if (group.admin!!) {
        throw ForbiddenException("Cannot delete admin group from deleteGroup method.")
      }
      isAdminOfGroup(group, securityContext, "Not owner of group, cannot delete") {
        groupApi.deleteGroup(gid)
        groupHolder.delete = true
      }
    }

    // revisit what we return
    return groupHolder.delete
  }

  override fun deletePersonFromGroup(
    gid: UUID, personId: UUID, holder: GroupServiceDelegate.DeletePersonFromGroupHolder,
    securityContext: SecurityContext
  ): Group {
    val groupHolder = GroupHolder()
    groupCheck(gid, authManager.from(securityContext)) { group: Group ->
      personCheck(personId) {
        isAdminOfGroup(
          group, securityContext, "No permission to delete user to group."
        ) {
          groupHolder.group = groupApi.deletePersonFromGroup(
            gid,
            personId,
            Opts().add(FillOpts.Members, holder.includeMembers)
          )
        }
      }
    }
    if (groupHolder.group == null) {
      throw NotFoundException()
    }
    return groupHolder.group!!
  }

  override fun findGroups(
    id: UUID,
    holder: GroupServiceDelegate.FindGroupsHolder,
    securityContext: SecurityContext
  ): List<Group> {
    val from = authManager.from(securityContext)
    if (authManager.isOrgAdmin(from) || authManager.isPortfolioGroupMember(id, from)) {
      return groupApi.findGroups(
        id, holder.filter, holder.order, Opts().add(
          FillOpts.People,
          holder.includePeople
        )
      )
    }
    throw ForbiddenException()
  }

  override fun getGroup(
    gid: UUID,
    holder: GroupServiceDelegate.GetGroupHolder,
    securityContext: SecurityContext
  ): Group {
    val opts =
      Opts().add(FillOpts.Acls, holder.includeGroupRoles).add(FilterOptType.Application, holder.byApplicationId)
    if (java.lang.Boolean.TRUE == holder.includeMembers) {
      opts.add(FillOpts.People)
      opts.add(FillOpts.Members)
    }
    return groupApi.getGroup(gid, opts, authManager.from(securityContext))
      ?: throw NotFoundException("No such group")
  }

  override fun getSuperuserGroup(
    id: UUID,
    securityContext: SecurityContext
  ): Group {
    return groupApi.getSuperuserGroup(id) ?: throw NotFoundException()
  }

  override fun updateGroupOnPortfolio(
    id: UUID,
    group: Group,
    holder: GroupServiceDelegate.UpdateGroupOnPortfolioHolder,
    securityContext: SecurityContext?
  ): Group {
    val groupHolder = GroupHolder()
    groupCheck(group.id, authManager.from(securityContext)) { groupCheck: Group ->
      isAdminOfGroup(groupCheck, securityContext!!, "No permission to rename group.") {
        try {
          groupHolder.group = groupApi.updateGroup(
            group.id,
            group,
            holder.applicationId,
            true == holder.updateMembers,
            true == holder.updateApplicationGroupRoles,
            true == holder.updateEnvironmentGroupRoles,
            Opts().add(FillOpts.Members, holder.includeMembers).add(FillOpts.Acls, holder.includeGroupRoles)
          )
        } catch (e: OptimisticLockingException) {
          throw WebApplicationException(422)
        } catch (e: GroupApi.DuplicateGroupException) {
          throw WebApplicationException(Response.Status.CONFLICT)
        } catch (e: DuplicateUsersException) {
          throw WebApplicationException(Response.Status.CONFLICT)
        }
      }
    }
    if (groupHolder.group == null) {
      throw NotFoundException()
    }
    return groupHolder.group!!

  }

  @Deprecated("Deprecated in Java")
  override fun updateGroup(
    gid: UUID,
    renameDetails: Group,
    holder: GroupServiceDelegate.UpdateGroupHolder,
    securityContext: SecurityContext
  ): Group {
    val groupHolder = GroupHolder()
    groupCheck(gid, authManager.from(securityContext)) { group: Group ->
      isAdminOfGroup(group, securityContext, "No permission to rename group.") {
        try {
          groupHolder.group = groupApi.updateGroup(
            gid,
            renameDetails,
            holder.applicationId,
            java.lang.Boolean.TRUE == holder.updateMembers,
            java.lang.Boolean.TRUE == holder.updateApplicationGroupRoles,
            java.lang.Boolean.TRUE == holder.updateEnvironmentGroupRoles,
            Opts().add(FillOpts.Members, holder.includeMembers).add(FillOpts.Acls, holder.includeGroupRoles)
          )
        } catch (e: OptimisticLockingException) {
          throw WebApplicationException(422)
        } catch (e: GroupApi.DuplicateGroupException) {
          throw WebApplicationException(Response.Status.CONFLICT)
        } catch (e: DuplicateUsersException) {
          throw WebApplicationException(Response.Status.CONFLICT)
        }
      }
    }
    if (groupHolder.group == null) {
      throw NotFoundException()
    }
    return groupHolder.group!!
  }
}
