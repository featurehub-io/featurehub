package io.featurehub.db.services

import io.ebean.Database
import io.ebean.DuplicateKeyException
import io.ebean.annotation.Transactional
import io.featurehub.db.api.*
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

@Singleton
open class GroupSqlApi @Inject constructor(
  private val database: Database,
  private val convertUtils: Conversions,
  private val archiveStrategy: ArchiveStrategy
) : GroupApi, InternalGroupSqlApi {

  override fun isPersonMemberOfPortfolioGroup(portfolioId: UUID, personId: UUID): Boolean {
    return convertUtils.isPersonMemberOfPortfolioGroup(portfolioId, personId)
  }

  private fun isPersonMemberOfPortfolioAdminGroup(portfolio: DbPortfolio?, personId: UUID): Boolean {
    return if (portfolio == null) {
      false
    } else QDbGroup().owningPortfolio
      .eq(portfolio).adminGroup
      .isTrue.groupMembers.person.id
      .eq(personId)
      .exists()
  }

  override fun getSuperuserGroup(orgId: UUID): Group? {
    val g = QDbGroup().owningOrganization.id
      .eq(orgId).adminGroup
      .isTrue.owningPortfolio
      .isNull.groupMembers.person
      .fetch()
      .findOne()
    return if (g != null) { // make sure you are a user in at least one group otherwise you can't see this
      // group
      convertUtils.toGroup(g, Opts.opts(FillOpts.Members))!!
    } else null
  }

  override fun groupsPersonOrgAdminOf(pId: UUID): List<Group> {
    return QDbGroup().whenArchived
      .isNull.owningPortfolio
      .isNull.adminGroup
      .eq(true).groupMembers.person.id
      .eq(pId)
      .findList()
      .map { g: DbGroup? -> convertUtils.toGroup(g, Opts.empty())!! }
  }

  override fun orgsUserIn(personId: UUID): List<Organization> {
    return QDbOrganization()
      .or()
      .and().group.whenArchived
      .isNull.group.groupMembers.person.id
      .eq(personId)
      .endAnd()
      .and().portfolios.groups.whenArchived
      .isNull.portfolios.groups.groupMembers.person.id
      .eq(personId)
      .endAnd()
      .endOr()
      .findList()
      .map { o: DbOrganization? -> convertUtils.toOrganization(o, Opts.empty())!! }
  }

  override fun createOrgAdminGroup(orgId: UUID, groupName: String, whoCreated: Person): Group? {
    val org = QDbOrganization().id.eq(orgId).findOne()
    if (org == null
      || QDbGroup().whenArchived
        .isNull.owningPortfolio
        .isNull.owningOrganization.id
        .eq(orgId)
        .exists()
    ) {
      return null // already exists or org doesn't exist
    }
    val dbGroup = DbGroup.Builder()
      .name(groupName)
      .adminGroup(true)
      .whoCreated(convertUtils.byPerson(whoCreated))
      .owningOrganization(org).build()

    saveGroup(dbGroup)

    return convertUtils.toGroup(dbGroup, Opts.empty())!!
  }

  private fun adminGroupExistsForPortfolio(portfolioId: UUID): Boolean {
    return QDbGroup()
      .whenArchived.isNull
      .owningPortfolio.id.eq(portfolioId)
      .adminGroup.isTrue
      .exists()
  }

  @Throws(GroupApi.DuplicateGroupException::class)
  override fun createGroup(portfolioId: UUID, group: Group, whoCreated: Person?): Group? {

    val portfolio = convertUtils.byPortfolio(portfolioId) ?: return null

    // an admin portfolio already exists for this portfolio
    if (group.admin == true && adminGroupExistsForPortfolio(portfolioId)) {
      return null
    }

    val isAdmin = (group.admin == true)

    // the group may pass application level permissions, so we need to set those up. It can pass in
    // a collection of applications, we need to ensure those applications belong to the right portfolio
    // and the application ids are valid
    val personCreatedId = convertUtils.byPerson(whoCreated)
    val acls: MutableSet<DbAcl> = HashSet()

    group.applicationRoles?.let { appRoles ->
      val matchingApps = QDbApplication()
        .select(QDbApplication.Alias.id, QDbApplication.Alias.portfolio.id)
        .portfolio.id.eq(portfolioId)
        .id.`in`(appRoles.map { it.applicationId }.toList()).findList().associateBy { it.id }

      appRoles.forEach { appRole ->
        matchingApps[appRole.applicationId]?.let { foundApplication ->
          acls.add(
            DbAcl.Builder()
              .application(foundApplication)
              .roles(appRolesToString(appRole.roles))
              .build()
          )
        }
      }
    }

    // no environment roles as yet
    val dbGroup = DbGroup.Builder()
      .owningPortfolio(portfolio)
      .owningOrganization(portfolio.organization)
      .adminGroup(isAdmin)
      .name(group.name)
      .whoCreated(personCreatedId)
      .groupRolesAcl(acls)
      .build()

    try {
      saveGroup(dbGroup)
    } catch (dke: DuplicateKeyException) {
      throw GroupApi.DuplicateGroupException()
    }

    if (dbGroup.isAdminGroup) {
      copySuperusersToPortfolioGroup(dbGroup)
    }

    return convertUtils.toGroup(dbGroup, Opts.empty())!!
  }

  override fun superuserGroup(org: DbOrganization): DbGroup? {
    return QDbGroup().whenArchived
      .isNull.owningOrganization
      .eq(org).owningPortfolio
      .isNull.adminGroup
      .isTrue
      .findOne()
  }

  private fun superuserGroupMembers(org: DbOrganization): List<DbPerson> {
    val superuserGroup = superuserGroup(org)
    return QDbPerson().whenArchived.isNull.groupMembers.group.eq(superuserGroup).findList()
  }

  private fun isSuperuser(org: DbOrganization?, person: DbPerson?): Boolean {
    if (org == null || person == null) {
      return false
    }
    val superuserGroup = superuserGroup(org)
    return QDbPerson().groupMembers.group.eq(superuserGroup).id.eq(person.id).exists()
  }

  @Transactional
  private fun copySuperusersToPortfolioGroup(dbGroup: DbGroup) {
    superuserGroupMembers(dbGroup.owningPortfolio.organization)
      .forEach { p: DbPerson ->
          if (!QDbGroupMember().person.id.eq(p.id).group.id.eq(dbGroup.id)
              .exists()
          ) {
            DbGroupMember(DbGroupMemberKey(p.id, dbGroup.id)).save()
          }
        }
  }

  override fun addPersonToGroup(groupId: UUID, personId: UUID, opts: Opts): Group? {
    val dbGroup = QDbGroup().id
      .eq(groupId).whenArchived
      .isNull
      .findOne() // no adding people to archived groups

    if (dbGroup != null) {
      val person = QDbPerson().id.eq(personId).whenArchived.isNull.findOne()
      if (person != null) {
        val groupFinder = QDbGroup().id.eq(groupId).groupMembers.person.id.eq(personId)
        if (opts.contains(FillOpts.Members)) {
          groupFinder.groupMembers.person.fetch() // ensure we prefetch the users in the group
        }
        val one = groupFinder.findOne()
        return if (one == null) {
          // ebean ensures this is never null
          updateGroupMembership(dbGroup, person)
          convertUtils.toGroup(dbGroup, opts)!!
        } else { // they are already in the group
          convertUtils.toGroup(one, opts)!!
        }
      }
    }

    return null
  }

  @Transactional
  private fun updateGroupMembership(dbGroup: DbGroup, person: DbPerson) {
    if (!QDbGroupMember().person.id.eq(person.id).group.id.eq(dbGroup.id).exists()) {
      DbGroupMember(DbGroupMemberKey(person.id, dbGroup.id)).save()
    }

    // they actually got added from the superusers group, so
    // lets update the portfolios
    if (dbGroup.isAdminGroup && dbGroup.owningPortfolio == null) {
      val sc = SuperuserChanges(dbGroup.owningOrganization)
      sc.addedSuperusers.add(person)
      sc.ignoredGroups.add(dbGroup.id)
      updateSuperusersFromPortfolioGroups(sc)
    }
  }

  override fun getGroup(gid: UUID, opts: Opts, person: Person): Group? {
    var eq = QDbGroup().id.eq(gid).groupMembers.person.fetch()

    if (!opts.contains(FillOpts.Archived)) {
      eq = eq.whenArchived.isNull
    }

    val one = eq.findOne()

    return if (one != null
      && (QDbGroup().id.eq(gid).groupMembers.person.whenArchived.isNull.groupMembers.person.id.eq(
        person.id!!.id
      ).exists()
        || isSuperuser(one.findOwningOrganisation(), convertUtils.byPerson(person))
        || isPersonMemberOfPortfolioAdminGroup(
        one.owningPortfolio, person.id!!.id
      ))
    ) {
      convertUtils.toGroup(one, opts)!!
    } else null
  }

  override fun findPortfolioAdminGroup(portfolioId: UUID, opts: Opts): Group? {
    var groupFinder = QDbGroup().owningPortfolio.id.eq(portfolioId)

    groupFinder = groupFinder.adminGroup.isTrue

    if (opts.contains(FillOpts.Members)) {
      groupFinder.groupMembers.person.fetch() // ensure we prefetch the users in the group
    }

    return convertUtils.toGroup(groupFinder.findOne(), opts)
  }

  override fun findOrganizationAdminGroup(orgId: UUID, opts: Opts): Group? {
    Conversions.nonNullOrganisationId(orgId)

    // there is only 1
    return convertUtils.toGroup(
      QDbGroup()
        .whenArchived.isNull
        .owningPortfolio.isNull
        .owningOrganization.id.eq(orgId)
        .owningOrganization.fetch(QDbOrganization.Alias.id)
        .adminGroup.isTrue
        .findOne(),
      opts
    )
  }

  override fun groupsWherePersonIsAnAdminMember(personId: UUID): List<Group> {
    return QDbGroup()
      .whenArchived.isNull
      .groupMembers.person.id.eq(personId)
      .adminGroup.isTrue
      .findList()
      .map { g: DbGroup? -> convertUtils.toGroup(g, Opts.empty())!! }
  }

  @Transactional
  override fun deleteGroup(gid: UUID) {
    Conversions.nonNullGroupId(gid)

    val group = QDbGroup().id.eq(gid).findOne()

    if (group != null) {
      archiveStrategy.archiveGroup(group)
    }
  }

  override fun deletePersonFromGroup(groupId: UUID, personId: UUID, opts: Opts): Group? {
    val person = convertUtils.byPerson(personId)
    if (person != null) {
      val member =
        QDbGroupMember().person.id.eq(personId).group.id.eq(groupId).group.whenArchived.isNull.group.fetch()
          .findOne() ?: return null
      val group = member.group

      // if it is an admin portfolio group and they are a superuser, you can't remove them
      if (group.isAdminGroup && group.owningPortfolio != null && isSuperuser(
          group.owningPortfolio.organization,
          person
        )
      ) {
        return null
      }
      deleteMember(member)

      // they actually got removed from the superusers group, so lets update the portfolios
      if (group.isAdminGroup && group.owningPortfolio == null) {
        val sc = SuperuserChanges(group.owningOrganization)
        sc.removedSuperusers.add(person.id)
        updateSuperusersFromPortfolioGroups(sc)
      }
      return convertUtils.toGroup(group, opts)!!
    }
    return null
  }

  @Transactional
  private fun deleteMember(member: DbGroupMember) {
    member.delete()
  }

  @Transactional
  private fun saveGroup(group: DbGroup) {
    database.save(group)
  }

  @Throws(OptimisticLockingException::class, GroupApi.DuplicateGroupException::class, DuplicateUsersException::class)
  override fun updateGroup(
    gid: UUID,
    gp: Group,
    updateMembers: Boolean,
    updateApplicationGroupRoles: Boolean,
    updateEnvironmentGroupRoles: Boolean,
    opts: Opts
  ): Group? {
    val group = convertUtils.byGroup(gid, opts)

    if (group != null && group.whenArchived == null) {
      if (gp.version == null || group.version != gp.version) {
        throw OptimisticLockingException()
      }

      gp.name?.let {
        group.name = it
      }

      transactionalGroupUpdate(gp, updateMembers, updateApplicationGroupRoles, updateEnvironmentGroupRoles, group)
      return convertUtils.toGroup(group, opts)!!
    }
    return null
  }

  // there are too many updates
  @Transactional
  @Throws(DuplicateUsersException::class, GroupApi.DuplicateGroupException::class)
  private fun transactionalGroupUpdate(
    gp: Group,
    updateMembers: Boolean,
    updateApplicationGroupRoles: Boolean,
    updateEnvironmentGroupRoles: Boolean,
    group: DbGroup
  ) {
    var superuserChanges: SuperuserChanges? = null
    if (gp.members != null && updateMembers) {
      superuserChanges = updateMembersOfGroup(gp, group)
    }
    var aclUpdates: AclUpdates? = null
    if (gp.environmentRoles != null && updateEnvironmentGroupRoles) {
      aclUpdates = updateEnvironmentMembersOfGroup(gp.environmentRoles, group)
    }
    if (gp.applicationRoles != null && updateApplicationGroupRoles) {
      updateApplicationMembersOfGroup(gp.applicationRoles, group)
    }
    try {
      updateGroup(group, aclUpdates)
    } catch (dke: DuplicateKeyException) {
      throw GroupApi.DuplicateGroupException()
    }
    superuserChanges?.let { updateSuperusersFromPortfolioGroups(it) }
  }

  // now we have to walk all the way down and remove these people from all admin portfolio groups
  override fun updateSuperusersFromPortfolioGroups(
    superuserChanges: SuperuserChanges
  ) {
    for (group in QDbGroup()
      .select(QDbGroup.Alias.id)
      .adminGroup.isTrue
      .owningPortfolio.isNotNull
      .owningPortfolio.organization
      .eq(superuserChanges.organization)
      .findList()) {
      if (superuserChanges.ignoredGroups.contains(group.id)) {
        continue
      }

      // remove any superusers
      if (superuserChanges.removedSuperusers.isNotEmpty()) {
        QDbGroupMember().group.id.eq(group.id).person.id.`in`(superuserChanges.removedSuperusers).delete()
      }

      if (superuserChanges.addedSuperuserPersonIds.isNotEmpty()) {
        superuserChanges.addedSuperuserPersonIds.forEach { newSuperuserPersonId ->
          if (!QDbGroupMember().person.id.eq(newSuperuserPersonId).group.id.eq(group.id).exists()) {
            DbGroupMember(DbGroupMemberKey(newSuperuserPersonId, group.id)).save()
          }
        }
      }

      // add superusers but only if they aren't there already
      if (superuserChanges.addedSuperusers.isNotEmpty()) {
        for (p in superuserChanges.addedSuperusers) {
          if (!QDbGroupMember().person.id.eq(p.id).group.id.eq(group.id).exists()) {
            DbGroupMember(DbGroupMemberKey(p.id, group.id)).save()
          }
        }
      }
    }
  }

  private fun updateGroup(group: DbGroup, aclUpdates: AclUpdates?) {
    database.update(group)
    if (aclUpdates != null) {
      if (aclUpdates.updates.isNotEmpty()) {
        database.updateAll(aclUpdates.updates)
      }
      if (aclUpdates.deletes.isNotEmpty()) {
        database.deleteAll(aclUpdates.deletes)
      }
      if (aclUpdates.creates.isNotEmpty()) {
        database.saveAll(aclUpdates.creates)
      }
    }
  }

  private fun updateApplicationMembersOfGroup(
    updatedApplicationRoles: List<ApplicationGroupRole>?, group: DbGroup
  ) {
    val desiredApplications: MutableMap<UUID, ApplicationGroupRole> = HashMap()
    val addedApplications: MutableSet<UUID> = HashSet()
    updatedApplicationRoles!!
      .forEach { role: ApplicationGroupRole ->
        desiredApplications[role.applicationId] = role
        addedApplications.add(role.applicationId) // ensure uniqueness
      }
    val removedAcls: MutableList<DbAcl> = ArrayList()
    group
      .groupRolesAcl
      .forEach { acl: DbAcl ->
          // leave the application acl's alone
          if (acl.application != null) {
            val egr = desiredApplications[acl.application.id]
            if (egr == null) { // we have it but we don't want it
              //          log.info("removing acl {}", acl);
              removedAcls.add(acl)
            } else {
              // don't add this one, we already have it
              addedApplications.remove(egr.applicationId)
              // change the roles if necessary
              resetApplicationAcl(acl, egr)
            }
          }
        }

    // delete ones that are no longer valid
    group.groupRolesAcl.removeAll(removedAcls)

    // add ones that we want
    for (ae in addedApplications) {
      val app = convertUtils.byApplication(ae)
      if (app != null && app.portfolio.id == group.owningPortfolio.id) {
        val acl = DbAcl.Builder().application(app).group(group).build()
        desiredApplications[ae]?.let { egr ->
          resetApplicationAcl(acl, egr)
        }

        group.groupRolesAcl.add(acl)
      }
    }
  }

  private fun resetApplicationAcl(acl: DbAcl, egr: ApplicationGroupRole) {
    val newRoles = egr.roles.map { obj: ApplicationRoleType -> obj.name }.sorted().joinToString { "," }

    if (acl.roles == null || newRoles != acl.roles) {
      acl.roles = newRoles
    }
  }

  override fun adminGroupsPersonBelongsTo(personId: UUID): List<DbGroup> {
    val groups = QDbGroup().select(QDbGroup.Alias.owningPortfolio.id,
      QDbGroup.Alias.adminGroup)
      .adminGroup.isTrue  // only admin groups
      .groupMembers.person.id.eq(personId)
      .findList()

    return groups
  }

  // we do it as a set of acl updates as a group can get a lot of permissions
  // across a lot of different environments, so its terribly inefficient to get
  // all of the environment acls for a group, best to get just the ones we are
  // updating
  private class AclUpdates {
    val updates = mutableListOf<DbAcl>()
    val deletes = mutableListOf<DbAcl>()
    val creates = mutableListOf<DbAcl>()
  }

  private fun updateEnvironmentMembersOfGroup(
    environmentRoles: List<EnvironmentGroupRole>?, group: DbGroup
  ): AclUpdates {
    val desiredEnvironments: MutableMap<UUID, EnvironmentGroupRole> = HashMap()
    val addedEnvironments: MutableSet<UUID> = HashSet()
    val aclUpdates = AclUpdates()

    environmentRoles!!
      .filter { r: EnvironmentGroupRole -> r.environmentId != null }
      .forEach { role: EnvironmentGroupRole ->
        desiredEnvironments[role.environmentId] = role
        addedEnvironments.add(role.environmentId) // ensure uniqueness
      }

    QDbAcl().group
      .eq(group).environment.id
      .`in`(desiredEnvironments.keys)
      .findEach { acl: DbAcl ->
        // leave the application acl's alone
        if (acl.environment != null) {
          val egr = desiredEnvironments[acl.environment.id]

          // don't add this one, we already have it, we just need to update it
          addedEnvironments.remove(acl.environment.id)

          // if we have no roles, we need to remove the ACL
          if (egr!!.roles == null || egr.roles.isEmpty()) {
            aclUpdates.deletes.add(acl)
          } else {
            // change the roles if necessary
            resetEnvironmentAcl(acl, egr)
            aclUpdates.updates.add(acl)
          }
        }
      }

    // add ones the new ones
    for (ae in addedEnvironments) {
      val egr = desiredEnvironments[ae]
      if (egr!!.roles != null && !egr.roles.isEmpty()) {
        val env = convertUtils.byEnvironment(
          ae, Opts.opts(FillOpts.ApplicationIds, FillOpts.PortfolioIds)
        )
        if (env != null
          && (env.parentApplication
            .portfolio
            .id
            == group.owningPortfolio.id)
        ) {
          val acl = DbAcl.Builder().environment(env).group(group).build()
          resetEnvironmentAcl(acl, egr)
          aclUpdates.creates.add(acl)
        } else {
          log.error(
            "Attempting to add an environment that doesn't exist or doesn't belong to the same portfolio {}",
            ae
          )
        }
      }
    }
    return aclUpdates
  }

  private fun resetEnvironmentAcl(acl: DbAcl, egr: EnvironmentGroupRole?) {
    val newRoles = egr!!.roles.stream().map { obj: RoleType -> obj.name }.sorted().collect(Collectors.joining(","))
    if (acl.roles == null || newRoles != acl.roles) {
      acl.roles = newRoles
    }
  }

  @Throws(DuplicateUsersException::class)
  private fun updateMembersOfGroup(gp: Group, group: DbGroup): SuperuserChanges? {
    val uuids = gp.members!!
      .filter { p: Person -> p.id != null }
      .map { p: Person ->
        p.id!!
          .id
      }.toSet()
    if (uuids.size != gp.members!!.size) {
      throw DuplicateUsersException()
    }
    val desiredPeople = gp.members!!
      .filter { p: Person -> p.id != null }
      .associateBy {  it.id!!.id }

    // if this is the superuser group, we will have to remove these people from all portfolio groups
    // as well
    val removedPerson = mutableListOf<UUID>()

    // ensure no duplicates get through
    val addedPeople = gp.members?.mapNotNull { it?.id?.id }?.toMutableSet() ?: mutableSetOf()

    val isSuperuserGroup = group.isAdminGroup && group.owningPortfolio == null

    val superusers =
      if (group.isAdminGroup && !isSuperuserGroup) superuserGroupMembers(group.owningPortfolio.organization)
        .map { obj: DbPerson -> obj.id } else listOf()

    QDbGroupMember().group.id.eq(group.id).person.fetch(QDbPerson.Alias.id).findList().forEach { member: DbGroupMember ->
        val personId = member.person.id
        val p = desiredPeople[personId]
        if (p == null) { // delete them
          // can't delete superusers from portfolio group. if this is the superusergroup or
          // isn't an admin group, superusers will be empty
          if (!superusers.contains(personId)) {
            removedPerson.add(personId)
          }
        } else {
          addedPeople.remove(personId) // they are already there, remove them from list to add
        }
      }

    QDbGroupMember().group.id.eq(group.id).person.id.`in`(removedPerson).delete()

    val actuallyAddedPeople = mutableListOf<DbPerson>()
    addedPeople.forEach { p: UUID? ->
        val person = convertUtils.byPerson(p)
        if (person != null) {
          if (!QDbGroupMember().person.id.eq(person.id).group.id.eq(group.id).exists()) {
            DbGroupMember(DbGroupMemberKey(person.id, group.id)).save()
            actuallyAddedPeople.add(person)
          }
        }
      }

    if (isSuperuserGroup) {
      val sc = SuperuserChanges(group.owningOrganization)
      sc.removedSuperusers.addAll(removedPerson)
      sc.addedSuperusers.addAll(actuallyAddedPeople)
      return sc
    }

    return null
  }

  override fun findGroups(portfolioId: UUID, filter: String?, ordering: SortOrder?, opts: Opts): List<Group> {
    Conversions.nonNullPortfolioId(portfolioId)
    var gFinder = QDbGroup().owningPortfolio.id.eq(portfolioId)
    if (filter != null && filter.trim().isNotEmpty()) {
      gFinder = gFinder.name.ilike("%" + filter.trim() + "%")
    }

    if (ordering != null) {
      if (ordering == SortOrder.ASC) {
        gFinder = gFinder.order().name.asc()
      } else if (ordering == SortOrder.DESC) {
        gFinder = gFinder.order().name.desc()
      }
    }

    if (!opts.contains(FillOpts.Archived)) {
      gFinder = gFinder.whenArchived.isNull
    }

    return gFinder.findList()
      .map { g: DbGroup? -> convertUtils.toGroup(g, opts)!! }
  }

  override fun updateAdminGroupForPortfolio(portfolioId: UUID, name: String) {
    QDbGroup().whenArchived
      .isNull.owningPortfolio.id
      .eq(portfolioId)
      .and().adminGroup
      .isTrue
      .endAnd()
      .findOne()?.let { group: DbGroup ->
        group.name = name
        saveGroup(group)
      }
  }

  companion object {
    private val log = LoggerFactory.getLogger(GroupSqlApi::class.java)

    @JvmStatic
    fun appRolesToString(roles: List<ApplicationRoleType>): String {

      return roles.map { obj -> obj.name }.sorted().joinToString(",")
    }
  }
}
