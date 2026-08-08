package io.featurehub.db.api

import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PortfolioGroupRoleType
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.UpdateGroup
import java.util.*

interface GroupApi {
  // checks if a person is a portfolio admin for this portfolio
  fun isPersonMemberOfPortfolioGroup(portfolioId: UUID, personId: UUID): Boolean
  // checks if any group in this portfolio has this user as a member
  fun isPersonMemberOfAnyPortfolioGroup(portfolioId: UUID, personId: UUID): Boolean
  fun getSuperuserGroup(orgId: UUID): Group?
  fun groupsPersonOrgAdminOf(personId: UUID): List<Group>
  fun orgsUserIn(personId: UUID): List<Organization>?
  fun portfolioRoles(personId: UUID, portfolio: UUID?): Set<PortfolioGroupRoleType>

  class CannotSetGroupManagerRoleOnAclGroup : RuntimeException()
  class DuplicateGroupException : Exception()

  /**
   * Creates top level admin group for a given organization
   * @param orgId
   * @param groupName - group name
   * @return Group with the group id
   */
  fun createOrgAdminGroup(orgId: UUID, groupName: String, whoCreated: Person): Group?

  /**
   * Creates a group - if "admin" is true, this will be a portfolio admin group unless there is already one
   * of those.
   */
  @Throws(DuplicateGroupException::class)
  fun createGroup(portfolioId: UUID, group: CreateGroup, whoCreated: Person?): Group?

  /**
   * Used only when the system itself needs to add a user, such as during initial setup or oauth/saml
   */
  fun systemAddPersonsToGroup(groupId: UUID, personIds: List<UUID>, opts: Opts): Group?
  fun addPersonsToGroup(groupId: UUID, personIds: List<UUID>, personAdding: UUID, opts: Opts): Group?
  fun getGroup(gid: UUID, opts: Opts, personId: UUID): Group?
  fun getGroup(gid: UUID, opts: Opts, person: Person): Group?
  fun findPortfolioAdminGroup(portfolioId: UUID, opts: Opts): Group?
  fun findOrganizationAdminGroup(orgId: UUID, opts: Opts): Group?
  fun groupsWherePersonIsAnAdminMember(personId: UUID): List<Group>
  fun deleteGroup(gid: UUID)
  fun deletePersonFromGroup(groupId: UUID, personId: UUID, opts: Opts): Group?

  @Throws(OptimisticLockingException::class, DuplicateGroupException::class, DuplicateUsersException::class)
  fun updateGroup(
    gid: UUID,
    group: UpdateGroup,
    appId: UUID?,
    updateApplicationGroupRoles: Boolean,
    updateEnvironmentGroupRoles: Boolean,
    updatedByPersonId: UUID,
    opts: Opts
  ): Group?

  fun findGroups(portfolioId: UUID, filter: String?, ordering: SortOrder?, opts: Opts): List<Group>
  fun updateAdminGroupForPortfolio(portfolioId: UUID, name: String)

  @Deprecated(replaceWith = ReplaceWith("updateGroup"), message = "Does not enforce group member management")
  @Throws(OptimisticLockingException::class, DuplicateGroupException::class, DuplicateUsersException::class)
  fun updateGroupV1(
    gid: UUID,
    group: Group,
    appId: UUID?,
    updateMembers: Boolean,
    updateApplicationGroupRoles: Boolean,
    updateEnvironmentGroupRoles: Boolean,
    personMakingUpdate: UUID,
    opts: Opts
  ): Group?

  /**
   * This returns the UUID the group that the specified user is a group_member_manager of. A person
   * who is a group_member_manager can only update members of a group, they cannot create new groups
   * and they cannot change the group roles. To allow them to do would be a privilege escalation.
   */
  fun groupUserIsManagerOf(personId: UUID, portfolioId: UUID): UUID?

  /**
   * Used by GroupResource to determine if it needs to check what the client is asking for and what permissions it needs to check
   * across the portfolio. There are actually 3 states - the group doesn't exist or the owning portfolio is null
   * (superadmin group) or the group exists and the portfolio id is returned. The method does not distinguish the first two.
   */
  fun findPortfolioOfGroup(groupId: UUID): UUID?
}
