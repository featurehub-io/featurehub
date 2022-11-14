package io.featurehub.db.api

import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Organization
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.SortOrder
import java.util.*

interface GroupApi {
  fun isPersonMemberOfPortfolioGroup(portfolioId: UUID, personId: UUID): Boolean
  fun getSuperuserGroup(orgId: UUID): Group?
  fun groupsPersonOrgAdminOf(personId: UUID): List<Group>
  fun orgsUserIn(personId: UUID): List<Organization>?

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
  fun createGroup(portfolioId: UUID, group: Group, whoCreated: Person?): Group?

  /**
   * Adds a person to a group
   * @param groupId
   * @param personId
   * @param opts
   * @return Group with the group id - default. Or plus opts if provided
   */
  fun addPersonToGroup(groupId: UUID, personId: UUID, opts: Opts): Group?
  fun getGroup(gid: UUID, opts: Opts, person: Person): Group?
  fun findPortfolioAdminGroup(portfolioId: UUID, opts: Opts): Group?
  fun findOrganizationAdminGroup(orgId: UUID, opts: Opts): Group?
  fun groupsWherePersonIsAnAdminMember(personId: UUID): List<Group>
  fun deleteGroup(gid: UUID)
  fun deletePersonFromGroup(groupId: UUID, personId: UUID, opts: Opts): Group?

  @Throws(OptimisticLockingException::class, DuplicateGroupException::class, DuplicateUsersException::class)
  fun updateGroup(
    gid: UUID,
    group: Group,
    updateMembers: Boolean,
    updateApplicationGroupRoles: Boolean,
    updateEnvironmentGroupRoles: Boolean,
    opts: Opts
  ): Group?

  fun findGroups(portfolioId: UUID, filter: String?, ordering: SortOrder?, opts: Opts): List<Group>
  fun updateAdminGroupForPortfolio(id: UUID, name: String)
}
