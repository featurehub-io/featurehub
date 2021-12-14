package io.featurehub.db.services

import io.ebean.Database
import io.ebean.DuplicateKeyException
import io.ebean.annotation.Transactional
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.model.*
import io.featurehub.db.model.query.QDbAcl
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbServiceAccount
import io.featurehub.db.model.query.QDbServiceAccountEnvironment
import io.featurehub.db.publish.CacheSource
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

@Singleton
class ServiceAccountSqlApi @Inject constructor(
  private val database: Database,
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val archiveStrategy: ArchiveStrategy
) : ServiceAccountApi {
  override fun get(id: UUID, opts: Opts): ServiceAccount? {
    val eq = opts(QDbServiceAccount().id.eq(id), opts)
    return convertUtils.toServiceAccount(eq.findOne(), opts)
  }

  private fun opts(finder: QDbServiceAccount, opts: Opts?): QDbServiceAccount {
    var qFinder = finder
    if (opts!!.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL)) {
      qFinder = qFinder.serviceAccountEnvironments.fetch()
    }
    if (!opts.contains(FillOpts.Archived)) {
      qFinder = qFinder.whenArchived.isNull
    }
    return qFinder
  }

  @Throws(OptimisticLockingException::class)
  override fun update(
    serviceAccountId: UUID,
    updater: Person,
    serviceAccount: ServiceAccount,
    opts: Opts
  ): ServiceAccount? {
    val sa = QDbServiceAccount().id.eq(serviceAccountId).whenArchived.isNull.findOne() ?: return null
    if (serviceAccount.version == null || serviceAccount.version != sa.version) {
      throw OptimisticLockingException()
    }
    val updatedEnvironments: MutableMap<UUID, ServiceAccountPermission> = HashMap()
    val newEnvironments: MutableList<UUID> = ArrayList()
    serviceAccount
      .permissions!!.forEach { perm: ServiceAccountPermission ->
        updatedEnvironments[perm.environmentId] = perm
        newEnvironments.add(perm.environmentId)
      }
    val deletePerms: MutableList<DbServiceAccountEnvironment?> = ArrayList()
    val updatePerms: MutableList<DbServiceAccountEnvironment?> = ArrayList()
    val createPerms: MutableList<DbServiceAccountEnvironment?> = ArrayList()

    // we drop out of this knowing which perms to delete and update
    QDbServiceAccountEnvironment().environment.id
      .`in`(updatedEnvironments.keys).serviceAccount
      .eq(sa)
      .findEach { upd: DbServiceAccountEnvironment ->
        val envId = upd.environment.id
        val perm = updatedEnvironments[envId]
        newEnvironments.remove(envId)
        if (perm == null || perm.permissions.isEmpty()) {
          deletePerms.add(upd)
        } else {
          val newPerms = convertPermissionsToString(perm.permissions)
          if (newPerms != upd.permissions) {
            upd.permissions = newPerms
            updatePerms.add(upd)
          }
        }
      }

    // now we need to know which perms to add
    newEnvironments.forEach { envId: UUID ->
      val perm = updatedEnvironments[envId]
      if (perm?.permissions?.isEmpty() == false) {
        val env = convertUtils.byEnvironment(
          envId, Opts.opts(FillOpts.ApplicationIds, FillOpts.PortfolioIds)
        )
        if (env != null
          && (env.parentApplication
            .portfolio
            .id
            == sa.portfolio.id)
        ) {
          createPerms.add(
            DbServiceAccountEnvironment.Builder()
              .environment(env)
              .serviceAccount(sa)
              .permissions(convertPermissionsToString(perm.permissions))
              .build()
          )
        }
      }
    }
    if (serviceAccount.description != null) {
      sa.description = serviceAccount.description
    }
    updateServiceAccount(sa, deletePerms, updatePerms, createPerms)
    return convertUtils.toServiceAccount(sa, opts)
  }

  private fun convertPermissionsToString(permissions: List<RoleType>): String {
    return permissions.toSet().joinToString(",") { rt ->
      rt.value
    }
  }

  override fun search(
    portfolioId: UUID,
    filter: String?,
    applicationId: UUID?,
    currentPerson: Person,
    opts: Opts
  ): List<ServiceAccount> {
    val person = convertUtils.byPerson(currentPerson) ?: return listOf()
    var application: DbApplication? = null
    var personAdmin = false
    if (applicationId != null) {
      application = convertUtils.byApplication(applicationId)
      if (application != null) {
        personAdmin = convertUtils.isPersonApplicationAdmin(person, application)
      }
    }
    var qFinder = opts(QDbServiceAccount().portfolio.id.eq(portfolioId), opts)
    if (filter != null) {
      qFinder = qFinder.name.ilike(filter)
    }
    if (application != null) {
      qFinder = qFinder.portfolio.applications.eq(application)
      qFinder = opts(qFinder, opts)
    }

    // if they are an admin they have everything, otherwise spelunk through finding relevant ACLs
    val environmentPermissions: MutableList<DbAcl> = ArrayList()
    if (!personAdmin
      && application != null && (opts.contains(FillOpts.Permissions) || opts.contains(FillOpts.SdkURL))
    ) {
      environmentPermissions.addAll(
        QDbAcl().roles.notEqualTo("").environment.parentApplication.eq(application).environment.whenUnpublished.isNull.environment.whenArchived.isNull.group.peopleInGroup.eq(
          person
        ).environment.fetch(
          QDbEnvironment.Alias.id
        )
          .findList()
      )
    }
    return qFinder.findList().stream()
      .map { sa: DbServiceAccount? ->
        convertUtils.toServiceAccount(
          sa,
          opts,
          environmentPermissions
        )
      }
      .collect(Collectors.toList())
  }

  override fun resetApiKey(
    id: UUID,
    resetClientEvalApiKey: Boolean,
    resetServerEvalApiKey: Boolean
  ): ServiceAccount? {
    Conversions.nonNullServiceAccountId(id)
    val sa = QDbServiceAccount().id.eq(id).whenArchived.isNull.findOne() ?: return null
    if (resetServerEvalApiKey) {
      sa.apiKeyServerEval = newServerEvalKey()
    }
    if (resetClientEvalApiKey) {
      sa.apiKeyClientEval = newClientEvalKey()
    }
    updateOnlyServiceAccount(sa)
    asyncUpdateCache(sa, null)
    return convertUtils.toServiceAccount(sa, Opts.empty())
  }

  @Transactional
  override fun cleanupServiceAccountApiKeys() {
    if (QDbServiceAccount()
        .or().apiKeyClientEval
        .isNull.apiKeyServerEval
        .isNull
        .endOr()
        .exists()
    ) {
      log.info("Updating service account keys as incomplete.")
      QDbServiceAccount()
        .or().apiKeyClientEval
        .isNull.apiKeyServerEval
        .isNull
        .endOr()
        .findEach { sa: DbServiceAccount ->
          var updated = false
          if (sa.apiKeyClientEval == null) {
            updated = true
            sa.apiKeyClientEval = newClientEvalKey()
          }
          if (sa.apiKeyServerEval == null) {
            updated = true
            sa.apiKeyServerEval = newServerEvalKey()
          }
          if (updated) {
            database.update(sa)
          }
        }
    }
  }

  @Transactional
  override fun unpublishServiceAccounts(portfolioId: UUID, serviceAccounts: List<UUID>?): Int {
    var query = QDbServiceAccount().portfolio.id.eq(portfolioId)
    if (serviceAccounts != null) {
      query = query.id.`in`(serviceAccounts)
    }

    query.select(QDbServiceAccount.Alias.id).findList().forEach { sa ->
      cacheSource.deleteServiceAccount(sa.id)
    }

    return query.asUpdate().set("whenUnpublished", Instant.now()).update()
  }

  private fun newServerEvalKey(): String {
    return RandomStringUtils.randomAlphanumeric(40)
  }

  private fun newClientEvalKey(): String {
    return (RandomStringUtils.randomAlphanumeric(30)
      + "*"
      + RandomStringUtils.randomAlphanumeric(20))
  }

  @Throws(ServiceAccountApi.DuplicateServiceAccountException::class)
  override fun create(
    portfolioId: UUID,
    creator: Person,
    serviceAccount: ServiceAccount,
    opts: Opts
  ): ServiceAccount? {
    Conversions.nonNullPortfolioId(portfolioId)
    Conversions.nonNullPerson(creator)
    val who = convertUtils.byPerson(creator)
    val portfolio = convertUtils.byPortfolio(portfolioId)
    if (who == null || portfolio == null) return null
    val changedEnvironments: MutableList<DbEnvironment> = ArrayList()
    val envs = environmentMap(serviceAccount)

    // now where we actually find the environment, add it into the list
    val perms = serviceAccount.permissions!!
      .stream()
      .map { sap: ServiceAccountPermission ->
        val e = envs[sap.environmentId]
        if (e != null) {
          changedEnvironments.add(e)
          return@map DbServiceAccountEnvironment.Builder()
            .environment(e)
            .permissions(convertPermissionsToString(sap.permissions))
            .build()
        }
        null
      }
      .filter { obj: DbServiceAccountEnvironment? -> Objects.nonNull(obj) }
      .collect(Collectors.toSet())

    // now create the SA and attach the perms to form the links
    val sa = DbServiceAccount.Builder()
      .name(serviceAccount.name)
      .description(serviceAccount.description)
      .whoChanged(who)
      .apiKeyServerEval(newServerEvalKey())
      .apiKeyClientEval(newClientEvalKey())
      .serviceAccountEnvironments(perms)
      .portfolio(portfolio)
      .build()
    perms.forEach { p: DbServiceAccountEnvironment? -> p!!.serviceAccount = sa }
    try {
      save(sa)
      asyncUpdateCache(sa, changedEnvironments)
    } catch (dke: DuplicateKeyException) {
      log.warn("Duplicate service account {}", sa.name, dke)
      throw ServiceAccountApi.DuplicateServiceAccountException()
    }
    return convertUtils.toServiceAccount(sa, opts)
  }

  private fun environmentMap(serviceAccount: ServiceAccount?): Map<UUID, DbEnvironment> {
    // find all of the UUIDs in the environment list
    val envIds = serviceAccount!!.permissions!!.stream()
      .map { obj: ServiceAccountPermission -> obj.environmentId }
      .filter { obj: UUID? -> Objects.nonNull(obj) }
      .collect(Collectors.toList())

    // now find them in the db in one swoop using "in" syntax
    return QDbEnvironment().id.`in`(envIds).whenArchived.isNull.findList().associateBy { e -> e.id }
  }

  @Transactional
  private fun save(sa: DbServiceAccount) {
    database.save(sa)
  }

  @Transactional
  private fun updateOnlyServiceAccount(sa: DbServiceAccount) {
    database.update(sa)
  }

  @Transactional
  private fun updateServiceAccount(
    sa: DbServiceAccount,
    deleted: List<DbServiceAccountEnvironment?>,
    updated: List<DbServiceAccountEnvironment?>,
    created: List<DbServiceAccountEnvironment?>
  ) {
    database.update(sa)
    database.updateAll(updated)
    database.deleteAll(deleted)
    database.saveAll(created)
    val changed: MutableMap<UUID, DbEnvironment> = HashMap()
    deleted.forEach { e: DbServiceAccountEnvironment? -> changed[e!!.environment.id] = e.environment }
    updated.forEach { e: DbServiceAccountEnvironment? -> changed[e!!.environment.id] = e.environment }
    created.forEach { e: DbServiceAccountEnvironment? -> changed[e!!.environment.id] = e.environment }
    asyncUpdateCache(sa, changed.values)
  }

  // because this is an update or save, its no problem we send this out of band of this save/update.
  private fun asyncUpdateCache(
    sa: DbServiceAccount, changedEnvironments: Collection<DbEnvironment>?
  ) {
    cacheSource.updateServiceAccount(sa, PublishAction.UPDATE)
    if (changedEnvironments != null && !changedEnvironments.isEmpty()) {
      changedEnvironments.forEach { e: DbEnvironment? ->
        cacheSource.updateEnvironment(
          e,
          PublishAction.UPDATE
        )
      }
    }
  }

  @Transactional
  override fun delete(deleter: Person, serviceAccountId: UUID): Boolean {
    val sa = QDbServiceAccount().id.eq(serviceAccountId).whenArchived.isNull.findOne()
    if (sa != null) {
      archiveStrategy.archiveServiceAccount(sa)
      return true
    }
    return false
  }

  companion object {
    private val log = LoggerFactory.getLogger(ServiceAccountSqlApi::class.java)
  }
}
