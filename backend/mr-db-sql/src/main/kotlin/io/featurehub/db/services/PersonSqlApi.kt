package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonApi
import io.featurehub.db.model.DbGroup
import io.featurehub.db.model.DbGroupMember
import io.featurehub.db.model.DbGroupMemberKey
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.query.QDbGroupMember
import io.featurehub.db.model.query.QDbLogin
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.db.password.PasswordSalter
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.SortOrder
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors

@Singleton
open class PersonSqlApi @Inject constructor(
  private val database: Database,
  private val convertUtils: Conversions,
  private val archiveStrategy: ArchiveStrategy
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

  @Throws(OptimisticLockingException::class)
  fun updatePerson(person: Person, opts: Opts?, adminPerson: DbPerson?, p: DbPerson): Person? {
    if (person.version == null || p.version != person.version) {
      throw OptimisticLockingException()
    }

    if (person.name != null) {
      p.name = person.name
    }

    if (person.email != null) {
      p.email = person.email
    }

    val newGroupMembers = mutableListOf<DbGroupMember>()

    if (person.groups != null) {
      // we are going to need their groups to determine what they can do
      val admin = convertUtils.toPerson(adminPerson, Opts.opts(FillOpts.Groups))
      val adminSuperuser = admin!!.groups!!
        .stream().anyMatch { g: Group -> g.portfolioId == null && g.admin!! }

      var validPortfolios = admin.groups?.stream()
        ?.filter { g: Group -> g.portfolioId != null && g.admin!! }
        ?.map { obj: Group -> obj.portfolioId }
        ?.collect(Collectors.toList())

      if (!adminSuperuser && validPortfolios?.isEmpty() == true) {
        return null // why are they even here???
      }

      if (validPortfolios == null) {
        validPortfolios = listOf()
      }

      val removeGroups: MutableList<UUID> = ArrayList()

      val replacementGroupIds = person.groups!!.stream().map { obj: Group -> obj.id }
        .collect(Collectors.toList())

      val foundReplacementGroupIds: MutableList<UUID?> = ArrayList() // these are the ones we found

      p.groupMembers.forEach { gm: DbGroupMember ->
        val g = gm.group
        if (replacementGroupIds.contains(g.id)) { // this has been passed to us and it is staying
          foundReplacementGroupIds.add(g.id)
        } else if (adminSuperuser || g.owningPortfolio != null && validPortfolios.contains(g.owningPortfolio.id)) {
          // only if the admin is a superuser or this is a group in one of their portfolios will we honour it
          removeGroups.add(g.id)
        } else {
          log.warn("No permission to remove group {} from user {}", g, p.email)
        }
      }

      log.debug("Removing groups {} from user {}", removeGroups, p.email)
      // now remove them from these groups, we know this is valid
      p.groupMembers.removeIf { gm -> removeGroups.contains(gm.group.id) }
      replacementGroupIds.removeAll(foundReplacementGroupIds) // now this should be id's that we should consider adding
      log.debug("Attempting to add groups {} to user {} ", replacementGroupIds, p.email)
      // now we have to find the replacement groups and see if this user is allowed to add them
      replacementGroupIds.forEach { gid: UUID? ->
        val group = convertUtils.byGroup(gid, Opts.empty())
        if (group != null && (adminSuperuser ||
            group.owningPortfolio != null && validPortfolios.contains(group.owningPortfolio.id))
        ) {
          if (!QDbGroupMember().person.id.eq(p.id).group.id.eq(group.id).exists()) {
            newGroupMembers.add(DbGroupMember(DbGroupMemberKey(p.id, group.id)))
          }
        } else {
          log.warn("No permission to add group {} to user {}", group, p.email)
        }
      }
    }
    updatePerson(p, newGroupMembers)
    return convertUtils.toPerson(p, opts!!)
  }

  override fun search(
    filter: String?,
    sortOrder: SortOrder?,
    offset: Int,
    max: Int,
    opts: Opts
  ): PersonApi.PersonPagination {
    var searchOffset = Math.max(offset, 0)
    var searchMax = Math.min(max, MAX_SEARCH)
    searchMax = Math.max(searchMax, 1)

    // set the limits
    var search = QDbPerson().setFirstRow(searchOffset).setMaxRows(searchMax)

    // set the filter if anything, make sure it is case insignificant
    if (filter != null) {
      // name is mixed case, email is always lower case
      search = search.or().name.icontains(filter).email.contains(filter.lowercase(Locale.getDefault())).endOr()
    }

    if (sortOrder != null) {
      search = if (sortOrder == SortOrder.ASC) {
        search.order().name.asc()
      } else {
        search.order().name.desc()
      }
    }

    if (!opts.contains(FillOpts.Archived)) {
      search = search.whenArchived.isNull
    }

    val futureCount = search.findFutureCount()
    val futureList = search.findFutureList()
    val pagination = PersonApi.PersonPagination()

    return try {
      pagination.max = futureCount.get()
      val org = convertUtils.dbOrganization
      val dbPeople = futureList.get()
      pagination.people = dbPeople.stream().map { dbp: DbPerson? -> convertUtils.toPerson(dbp, org, opts) }
        .collect(Collectors.toList())
      val now = LocalDateTime.now()
      pagination.personIdsWithExpiredTokens = dbPeople.stream()
        .filter { p: DbPerson -> p.token != null && p.tokenExpiry != null && p.tokenExpiry.isBefore(now) }
        .map { obj: DbPerson -> obj.id }
        .collect(Collectors.toList())
      pagination.personsWithOutstandingTokens = dbPeople.stream()
        .filter { p: DbPerson -> p.token != null }
        .map { p: DbPerson -> PersonApi.PersonToken(p.token, p.id) }
        .collect(Collectors.toList())
      pagination
    } catch (e: InterruptedException) {
      log.error("Failed to execute search.", e)
      throw RuntimeException(e)
    } catch (e: ExecutionException) {
      log.error("Failed to execute search.", e)
      throw RuntimeException(e)
    }
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
    protected get() = LocalDateTime.now()

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
      updatePerson(person, null)
      personToken = PersonApi.PersonToken(person.token, person.id)
    } else if (onePerson.whenArchived != null) {
      onePerson.whenArchived = null
      onePerson.token = UUID.randomUUID().toString() // ensures it gets past registration again
      onePerson.name = name
      if (created != null) {
        onePerson.whoCreated = created
      }
      updatePerson(onePerson, null)
      return null
    } else {
      throw PersonApi.DuplicatePersonException()
    }
    return personToken
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
    return if (QDbPerson().email.eq(email.lowercase(Locale.getDefault())).findOne() == null) {
      val builder = DbPerson.Builder()
        .email(email.lowercase(Locale.getDefault()))
        .name(name)
      if (created != null) {
        builder.whoCreated(created)
      }
      val person = builder.build()
      passwordSalter.saltPassword(password, DbPerson.DEFAULT_PASSWORD_ALGORITHM)
        .ifPresent { password: String? -> person.password = password }
      person.passwordAlgorithm = DbPerson.DEFAULT_PASSWORD_ALGORITHM
      updatePerson(person, null)
      convertUtils.toPerson(person, opts!!)
    } else {
      throw PersonApi.DuplicatePersonException()
    }
  }

  @Transactional
  private fun updatePerson(p: DbPerson, newGroupMembers: MutableList<DbGroupMember>?) {
    database.save(p)
    if (newGroupMembers != null) {
      database.saveAll(newGroupMembers)
    }
  }

  override open fun delete(email: String): Boolean {
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
  }
}
