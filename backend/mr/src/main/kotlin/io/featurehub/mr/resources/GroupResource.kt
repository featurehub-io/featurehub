package io.featurehub.mr.resources

import io.featurehub.db.FilterOptType
import io.featurehub.db.api.*
import io.featurehub.mr.api.GroupServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.UpdateGroup
import io.featurehub.mr.utils.PortfolioUtils
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
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
  private val authManager: AuthManagerService,
  private val portfolioUtils: PortfolioUtils
) : GroupServiceDelegate {
  internal class GroupHolder {
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

  private fun personCheck(ids: List<UUID>, emailAddresses: List<String>, action: Consumer<Set<UUID>>): Set<UUID> {
    val matchedPersons = personApi.findPeople(ids, emailAddresses)
    if (matchedPersons.isNotEmpty()) {
      action.accept(matchedPersons)
    }
    return matchedPersons
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
        val currentUser = portfolioUtils.portfolioUserManager(securityContext, group.portfolioId)

        groupHolder.group =
          groupApi.addPersonsToGroup(
            gid, listOf(personId), currentUser, Opts()
              .add(FillOpts.Members, holder.includeMembers)
              .add(FillOpts.MembersV2, holder.includeMembersV2)
          )
      }
    }
    if (groupHolder.group == null) {
      throw NotFoundException()
    }
    return groupHolder.group!!
  }

  /**
   * This method is a little tricky because of the new GROUP_MEMBER_MANAGER. Such a person cannot add themselves or
   * another GROUP_MEMBER_MANAGER to a group that doesn't already have those permissions. Because this is quite complex
   * we devolve the responsibility to the DB layer as it knows what users this person is actually trying to add.
   */
  override fun addPersonsToGroup(
    gid: UUID,
    holder: GroupServiceDelegate.AddPersonsToGroupHolder,
    securityContext: SecurityContext
  ): Group {
    val groupHolder = GroupHolder()

    groupCheck(
      gid,
      authManager.from(securityContext)
    ) { group: Group ->
      personCheck(holder.personId ?: emptyList(), holder.email ?: emptyList()) { personIds ->
        val personId = portfolioUtils.portfolioUserManager(securityContext, group.portfolioId)

        groupHolder.group =
          groupApi.addPersonsToGroup(
            gid, personIds.toList(),
            personId,
            Opts().add(FillOpts.MembersV2, holder.includeMembersV2)
          )
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
    securityContext: SecurityContext
  ): Group {
    val current = authManager.from(securityContext)
    // only admins can create groups
    portfolioUtils.portfolioAdmin(securityContext, id)

    return try {
      groupApi.createGroup(id, createGroup, current) ?: throw NotFoundException()
    } catch (_: GroupApi.DuplicateGroupException) {
      throw WebApplicationException(Response.Status.CONFLICT)
    }
  }

  override fun deleteGroup(
    gid: UUID,
    holder: GroupServiceDelegate.DeleteGroupHolder,
    securityContext: SecurityContext
  ): Boolean {
    val groupHolder = GroupHolder()
    groupCheck(gid, authManager.from(securityContext)) { group: Group ->
      val portfolioId = group.portfolioId ?: throw ForbiddenException("cannot delete superuser group")

      portfolioUtils.portfolioAdmin(securityContext, portfolioId)

      if (group.admin!!) {
        throw ForbiddenException("Cannot delete admin group from deleteGroup method.")
      }

      groupApi.deleteGroup(gid)
      groupHolder.delete = true
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
        // is a person an admin, or a group member manager
        portfolioUtils.portfolioUserManager(securityContext, group.portfolioId)

        groupHolder.group = groupApi.deletePersonFromGroup(
          gid,
          personId,
          Opts()
            .add(FillOpts.Members, holder.includeMembers)
            .add(FillOpts.MembersV2, holder.includeMembersV2)
        )
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
    // is a person an admin, or a group member manager
    portfolioUtils.portfolioUserManager(securityContext, id)

    return groupApi.findGroups(
      id, holder.filter, holder.order, Opts().add(
        FillOpts.People,
        holder.includePeople
      )
    )
  }

  override fun getGroup(
    gid: UUID,
    holder: GroupServiceDelegate.GetGroupHolder,
    securityContext: SecurityContext
  ): Group {
    // a person can get their own groups details on ACLs and apps, but not members
    // unless they are an admin. If a non-admin includes these permissions they will get a 403
    // instead of a 404 for a non-existent group, this is an acceptable behaviour because they shouoldn't be asking
    if (holder.includeMembers == true || holder.includeMembersV2 == true) {
      // ensure they are a user manager or admin if they are asking for members
      portfolioUtils.portfolioUserManager(securityContext,
        groupApi.findPortfolioOfGroup(gid))
    }

    // the getGroup API in SQL enforces that only members of the group in some way can read the groups
    val opts =
      Opts().add(FillOpts.Acls, holder.includeGroupRoles)
        .add(FilterOptType.Application, holder.byApplicationId)
        .add(FillOpts.MembersV2, holder.includeMembersV2)
        .add(FillOpts.People, holder.includeMembers)
        .add(FillOpts.Members, holder.includeMembers)

    return groupApi.getGroup(gid, opts, authManager.from(securityContext))
      ?: throw NotFoundException("No such group")
  }

  fun updatePermissionCheck(groupId: UUID, securityContext: SecurityContext, portfolioId: UUID,updateApplicationGroupRoles: Boolean?, updateEnvironmentGroupRoles: Boolean?,
                            callback: (updateAppRoles: Boolean, updateEnvRoles: Boolean, personId: UUID,) -> Unit) {
    // superuser, portfolio admin only. No GMMs, they must use addUserToGroup
    val personId = portfolioUtils.portfolioAdmin(securityContext, portfolioId)

    val group = groupApi.getGroup(groupId, Opts.empty(), personId) ?: throw NotFoundException("No such group")

    // the group id does not match the portfolio id
    if (group.portfolioId != portfolioId) {
      throw BadRequestException()
    }

    callback( updateApplicationGroupRoles == true, updateEnvironmentGroupRoles == true, personId)
  }

  @Deprecated("Deprecated in Java")
  override fun updateGroupOnPortfolio(
    id: UUID,
    group: Group,
    holder: GroupServiceDelegate.UpdateGroupOnPortfolioHolder,
    securityContext: SecurityContext
  ): Group {
    val groupHolder = GroupHolder()
    // this API is not only deprecated, it will not support someone trying to use it who has a group member manager role.
    // this prevents us from having to support the complex user checking in the backend.

    updatePermissionCheck(group.id, securityContext, id, holder.updateApplicationGroupRoles,
      holder.updateEnvironmentGroupRoles
    ) { updateApplicationGroupRoles, updateEnvironmentGroupRoles, currentUserId ->

      try {
        groupHolder.group = groupApi.updateGroupV1(
          group.id,
          group,
          holder.applicationId,
          true == holder.updateMembers,
          updateApplicationGroupRoles,
          updateEnvironmentGroupRoles,
          currentUserId,
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

    if (groupHolder.group == null) {
      throw NotFoundException()
    }

    return groupHolder.group!!
  }

  override fun updateGroupOnPortfolioV2(
    id: UUID,
    group: UpdateGroup,
    holder: GroupServiceDelegate.UpdateGroupOnPortfolioV2Holder,
    securityContext: SecurityContext
  ): Group {
    val groupHolder = GroupHolder()
    updatePermissionCheck(group.id, securityContext, id, holder.updateApplicationGroupRoles,
      holder.updateEnvironmentGroupRoles
    ) { updateApplicationGroupRoles, updateEnvironmentGroupRoles, personId ->

      try {
        groupHolder.group = groupApi.updateGroup(
          group.id,
          group,
          holder.applicationId,
          updateApplicationGroupRoles,
          updateEnvironmentGroupRoles,
          personId,
          Opts()
            .add(FillOpts.Members, holder.includeMembers)
            .add(FillOpts.MembersV2, holder.includeMembersV2)
            .add(FillOpts.Acls, holder.includeGroupRoles)
        )
      } catch (_: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (_: GroupApi.DuplicateGroupException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (_: DuplicateUsersException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (_: GroupApi.CannotSetGroupManagerRoleOnAclGroup) {
        throw BadRequestException()
      }
    }

    if (groupHolder.group == null) {
      throw NotFoundException()
    }
    return groupHolder.group!!

  }
}
