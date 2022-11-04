package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.*
import io.featurehub.db.model.DbGroupMember
import io.featurehub.db.model.DbGroupMemberKey
import io.featurehub.db.model.DbLogin
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.query.QDbGroup
import io.featurehub.db.model.query.QDbGroupMember
import io.featurehub.db.model.query.QDbLogin
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.model.query.QGroupMemberAgg
import io.featurehub.db.password.PasswordSalter
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.PersonType
import io.featurehub.mr.model.SearchPerson
import io.featurehub.mr.model.SortOrder
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutionException

@Singleton
open class PersonSqlApi @Inject constructor(
  private val database: Database,
  private val convertUtils: Conversions,
  private val archiveStrategy: ArchiveStrategy,
  private val internalGroupSqlApi: InternalGroupSqlApi
) : PersonApi {
  private val passwordSalter = PasswordSalter()

  @Throws(OptimisticLockingException::class)
  override fun update(id: UUID, person: Person, opts: Opts, updatedBy: UUID): Person? {
    val adminPerson = convertUtils.byPerson(updatedBy, Opts.opts(FillOpts.Groups))
    val p = convertUtils.byPerson(id, opts)

    return if (adminPerson != null && p != null && adminPerson.whenArchived == null && p.whenArchived == null) {
      updatePerson(person, opts, adminPerson, p)!!
    } else null
  }

  override fun noUsersExist(): Boolean {
    return !QDbPerson().exists()
  }


  inner class GroupChangeCollection {
    val groupsToAdd = mutableListOf<UUID>()
    val groupsToRemove = mutableListOf<UUID>()

    override fun toString(): String {
      return "to remove $groupsToRemove - to add $groupsToAdd"
    }
  }

  @Throws(OptimisticLockingException::class)
  fun updatePerson(person: Person, opts: Opts?, adminPerson: DbPerson?, p: DbPerson): Person? {
    if (person.version == null || p.version != person.version) {
      throw OptimisticLockingException()
    }

    if (person.name != null) {
      p.name = person.name
    }

    // can update the email address of a non-service account
    if (person.email != null && p.personType != PersonType.SERVICEACCOUNT) {
      p.email = person.email
    }

    val groupChanges = GroupChangeCollection()
    val superuserChanges = SuperuserChanges(convertUtils.dbOrganization())

    if (person.groups != null) {
      // we are going to need their groups to determine what they can do
      val admin = convertUtils.toPerson(adminPerson, Opts.opts(FillOpts.Groups))
      val performedBySuperuser = admin!!.groups!!
        .stream().anyMatch { g: Group -> g.portfolioId == null && g.admin!! }

      var portfoliosPerformingUserCanManage = admin.groups
        ?.filter { g: Group -> g.portfolioId != null && g.admin!! }
        ?.map { obj: Group -> obj.portfolioId }

      if (!performedBySuperuser && portfoliosPerformingUserCanManage?.isEmpty() == true) {
        return null // why are they even here???
      }

      if (portfoliosPerformingUserCanManage == null) {
        portfoliosPerformingUserCanManage = listOf()
      }

      val superuserGroup = internalGroupSqlApi.superuserGroup(convertUtils.dbOrganization())

      val groupsAlreadyIn =
        QDbGroupMember().person.id.eq(p.id).select(QDbGroupMember.Alias.id.groupId).findList().map { it.id.groupId }
      val groupsTheyWant = person.groups!!.map { it.id!! }

      // we start with a list of groups they want and remove the list of groups they are already in
      // and that gives us the list of groups they want to add
      val groupsTheyWantToAdd = mutableListOf<UUID>()
      // add all the groups they have asked for
      groupsTheyWantToAdd.addAll(groupsTheyWant)
      // now remove from that list all the groups the are already in
      groupsTheyWantToAdd.removeAll(groupsAlreadyIn)

      if (!performedBySuperuser) { // if the superuser isn't doing this, we need to check if its ok
        removeChangeIfOutsidePortfolioPermission(groupsTheyWantToAdd, portfoliosPerformingUserCanManage)
      }

      if (groupsTheyWantToAdd.contains(superuserGroup?.id)) {
        superuserChanges.addedSuperusers.add(p)
      }

      // we start with a list of all the groups they are already in and subtract the groups they have told us they want.
      // the difference is the groups they no longer want
      val groupsTheyWantToRemove = mutableListOf<UUID>()
      groupsTheyWantToRemove.addAll(groupsAlreadyIn)
      groupsTheyWantToRemove.removeAll(groupsTheyWant)

      if (!performedBySuperuser) {
        removeChangeIfOutsidePortfolioPermission(groupsTheyWantToRemove, portfoliosPerformingUserCanManage)
      }

      if (groupsTheyWantToRemove.contains(superuserGroup?.id)) {
        superuserChanges.removedSuperusers.add(p.id)
      }

      groupChanges.groupsToAdd.addAll(groupsTheyWantToAdd)
      groupChanges.groupsToRemove.addAll(groupsTheyWantToRemove)

      log.debug("Changing groups for person ${p.id} as $groupChanges")
    }

    updatePerson(p, groupChanges, superuserChanges)

    return convertUtils.toPerson(p, opts!!)
  }

  private fun removeChangeIfOutsidePortfolioPermission(
    groupsTheyWantToAdd: MutableList<UUID>,
    portfoliosPerformingUserCanManage: List<@Valid UUID?>
  ) {
    val groupPortfolioMap =
      QDbGroup().id.`in`(groupsTheyWantToAdd)
        .select(QDbGroup.Alias.id, QDbGroup.Alias.owningPortfolio.id).findList()
        .associate { it.id!! to it.owningPortfolio.id }

    // now we have to work through them to ensure they are allowed to add them
    groupsTheyWantToAdd.removeIf { groupId ->
      !portfoliosPerformingUserCanManage.contains(groupPortfolioMap[groupId])
    }
  }

  override fun search(
    filter: String?, sortOrder: SortOrder?, offset: Int, max: Int,
    personTypes: Set<PersonType?>,
    opts: Opts
  ): PersonApi.PersonPagination {
    val searchOffset = offset.coerceAtLeast(0)
    val searchMax = max.coerceAtMost(MAX_SEARCH).coerceAtLeast(1)

    // set the limits
    var search = QDbPerson().setFirstRow(searchOffset).setMaxRows(searchMax)

    // set the filter if anything, make sure it is case insignificant
    if (filter != null) {
      val fil = filter.lowercase(Locale.getDefault())
      // name is mixed case, email is always lower case
      search = search.or().name.icontains(fil).email.contains(fil).endOr()
    }

    search = if (personTypes.isNotEmpty()) {
      if (personTypes.size == 1) search.personType.eq(personTypes.first()) else search.personType.`in`(personTypes)
    } else {
      search.personType.eq(PersonType.PERSON)
    }

    if (sortOrder != null) {
      search = if (sortOrder == SortOrder.ASC) {
        search.orderBy("upper(name) asc")
      } else {
        search.orderBy("upper(name) desc")
      }
    }

    if (!opts.contains(FillOpts.Archived)) {
      search = search.whenArchived.isNull
    }

    val futureCount = search.findFutureCount()
    val futureList = search.findFutureList()

    return try {
      val org = convertUtils.dbOrganization()
      val dbPeople = futureList.get()
      val people = dbPeople.map { dbp: DbPerson? -> convertUtils.toPerson(dbp, org, opts)!! }

      val peopleGroupCount: Map<UUID, Int> =
        if (opts.contains(FillOpts.CountGroups)) findGroupCounts(dbPeople) else emptyMap()

      PersonApi.PersonPagination(
        futureCount.get(),
        if (opts.contains(FillOpts.CountGroups)) people.map { p -> toSearchPerson(p, peopleGroupCount) } else emptyList(),
        if (!opts.contains(FillOpts.CountGroups)) people else emptyList(),
        dbPeople
          .filter { p -> p.token != null }
          .map { p -> PersonApi.PersonToken(p.token, p.id) },
        dbPeople
          .filter { p -> p.token != null && p.tokenExpiry != null && p.tokenExpiry.isBefore(now) }
          .map { obj -> obj.id },
      )
    } catch (e: InterruptedException) {
      log.error("Failed to execute search.", e)
      throw RuntimeException(e)
    } catch (e: ExecutionException) {
      log.error("Failed to execute search.", e)
      throw RuntimeException(e)
    }
  }

  private fun findGroupCounts(dbPeople: List<DbPerson>) =
    QGroupMemberAgg()
      .select(QGroupMemberAgg.Alias.personId, QGroupMemberAgg.Alias.counter)
      .personId.`in`(dbPeople.map { it.id }).findList().associate {
        it.personId to it.counter
      }



  override fun get(email: String, opts: Opts): Person? {
    if (email.contains("@")) {
      var search = QDbPerson().email.eq(email.lowercase(Locale.getDefault()))
      if (!opts.contains(FillOpts.Archived)) {
        search = search.whenArchived.isNull
      }
      return search.groupMembers.fetch()
        .findOneOrEmpty()
        .map { p: DbPerson? -> convertUtils.toPerson(p, opts) }
        .orElse(null)
    }
    val id = Conversions.checkUuid(email)
    return id?.let { get(it, opts) }
  }

  override fun get(id: UUID, opts: Opts): Person? {
    Conversions.nonNullPersonId(id)
    var search = QDbPerson().id.eq(id)
    if (!opts.contains(FillOpts.Archived)) {
      search = search.whenArchived.isNull
    }
    return search.groupMembers.fetch()
      .findOneOrEmpty()
      .map { p: DbPerson? -> convertUtils.toPerson(p, opts) }
      .orElse(null)
  }

  override fun getByToken(token: String, opts: Opts): Person? {
    val person = QDbPerson().whenArchived.isNull.token.eq(token).findOne()
    return if (person != null && person.tokenExpiry.isAfter(now)) {
      convertUtils.toPerson(person, opts)!!
    } else null
  }

  protected val now: LocalDateTime
  get() = LocalDateTime.now()

  @Throws(PersonApi.DuplicatePersonException::class)
  override fun create(email: String, name: String?, createdBy: UUID?): PersonApi.PersonToken? {
    val created = if (createdBy == null) null else convertUtils.byPerson(createdBy)
    if (createdBy != null && created == null) {
      return null
    }
    val personToken: PersonApi.PersonToken
    val onePerson = QDbPerson().email.eq(email.lowercase(Locale.getDefault())).findOne()
    if (onePerson == null) {
      val token = UUID.randomUUID().toString()
      val builder = DbPerson.Builder()
        .email(email.lowercase(Locale.getDefault()))
        .name(name)
        .token(token)
        .tokenExpiry(now.plusDays(7))
      if (created != null) {
        builder.whoCreated(created)
      }
      val person = builder.build()
      updatePerson(person)
      personToken = PersonApi.PersonToken(person.token, person.id)
    } else if (onePerson.whenArchived != null) {
      onePerson.whenArchived = null
      onePerson.token = UUID.randomUUID().toString() // ensures it gets past registration again
      onePerson.name = name
      onePerson.isPasswordRequiresReset = true
      if (created != null) {
        onePerson.whoCreated = created
      }
      updatePerson(onePerson)
      personToken = PersonApi.PersonToken(onePerson.token, onePerson.id)
    } else {
      throw PersonApi.DuplicatePersonException()
    }
    return personToken
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  override fun createServicePerson(name: String, createdBy: UUID?): CreatedServicePerson? {
    val created = if (createdBy == null) null else convertUtils.byPerson(createdBy)
    if (createdBy != null && created == null) {
      return null
    }

    val servicePerson = DbPerson()
    servicePerson.name = name
    with(servicePerson) {
      id = UUID.randomUUID()
      email = "${id}@admin.sa.featurehub.io"
      personType = PersonType.SERVICEACCOUNT
      whoChanged = created
      save()
    }

    val token = createAdminServiceAccountToken(servicePerson)

    return CreatedServicePerson(convertUtils.toPerson(servicePerson)!!, token)
  }

  private fun createAdminServiceAccountToken(person: DbPerson): String {
    val token = RandomStringUtils.randomAlphanumeric(48)
    DbLogin.Builder().person(person).token(token).lastSeen(Instant.now()).build().save()
    return token
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  override fun resetServicePersonToken(serviceAccountId: UUID): CreatedServicePerson? {
    val search = QDbPerson().id.eq(serviceAccountId)
      .personType.eq(PersonType.SERVICEACCOUNT)
      .whenArchived.isNull.findOne()

    if (search != null) {
      QDbLogin().person.eq(search).delete()
      val token = createAdminServiceAccountToken(search)
      return CreatedServicePerson(convertUtils.toPerson(search)!!, token)
    }

    return null
  }

  /**
   * This person will be fully formed, not token. Usually used only for testing.
   */
  @Throws(PersonApi.DuplicatePersonException::class)
  fun createPerson(email: String?, name: String?, password: String?, createdById: UUID?, opts: Opts?): Person? {
    if (email == null) {
      return null
    }
    val created = if (createdById == null) null else convertUtils.byPerson(createdById)
    if (createdById != null && created == null) {
      return null
    }
    return if (!QDbPerson().email.eq(email.lowercase(Locale.getDefault())).exists()) {
      val builder = DbPerson.Builder()
        .email(email.lowercase(Locale.getDefault()))
        .name(name)
      if (created != null) {
        builder.whoCreated(created)
      }
      val person = builder.build()
      passwordSalter.saltPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
        .ifPresent { pwd: String? -> person.password = pwd }
      person.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
      updatePerson(person)
      convertUtils.toPerson(person, opts!!)
    } else {
      throw PersonApi.DuplicatePersonException()
    }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun updatePerson(
    p: DbPerson,
    groupChangeCollection: GroupChangeCollection? = null,
    superuserChanges: SuperuserChanges? = null
  ) {
    database.save(p)

    if (groupChangeCollection != null) {
      if (groupChangeCollection.groupsToRemove.isNotEmpty()) {
        QDbGroupMember().person.id.eq(p.id).group.id.`in`(groupChangeCollection.groupsToRemove).delete()
      }

      if (groupChangeCollection.groupsToAdd.isNotEmpty()) {
        groupChangeCollection.groupsToAdd.forEach { g ->
          if (!QDbGroupMember().person.id.eq(p.id).group.id.eq(g).exists()) {
            DbGroupMember(DbGroupMemberKey(p.id, g)).save()
          }
        }
      }
    }

    if (superuserChanges != null) {
      internalGroupSqlApi.updateSuperusersFromPortfolioGroups(superuserChanges)
    }
  }

  override fun delete(email: String): Boolean {
    return QDbPerson().email.eq(email.lowercase(Locale.getDefault())).findOne()?.let { p ->
      archiveStrategy.archivePerson(p)
      // remove all of their tokens
      QDbLogin().person.id.eq(p.id).delete()
      true
    } ?: false
  }

  companion object {
    private val log = LoggerFactory.getLogger(PersonSqlApi::class.java)
    const val MAX_SEARCH = 100

    fun toSearchPerson(person: Person, groupCountsByPersonId: Map<UUID, Int>): SearchPerson {
      return SearchPerson()
        .personType(person.personType!!)
        .id(person.id!!.id)
        .email(person.email!!)
        .name(person.name!!)
        .whenLastAuthenticated(person.whenLastAuthenticated)
        .whenLastSeen(person.whenLastSeen)
        .groupCount(groupCountsByPersonId[person.id!!.id] ?: 0)
    }
  }
}
