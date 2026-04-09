package io.featurehub.db.services

import io.ebean.DuplicateKeyException
import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.model.*
import io.featurehub.db.model.query.QDbAcl
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureFilter
import io.featurehub.db.model.query.QDbServiceAccount
import io.featurehub.db.model.query.QDbServiceAccountEnvironment
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Singleton
class ServiceAccountSqlApi @Inject constructor(
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val archiveStrategy: ArchiveStrategy,
  private val internalPersonApi: InternalPersonApi
) : ServiceAccountApi, InternalServiceAccountApi {
  override fun get(id: UUID, opts: Opts): ServiceAccount? {
    val eq = opts(QDbServiceAccount().id.eq(id), opts)
    return convertUtils.toServiceAccount(eq.findOne(), opts)
  }

  private fun opts(finder: QDbServiceAccount, opts: Opts?): QDbServiceAccount {
    var qFinder = finder
    opts?.let { opt ->
      if (opt.contains(FillOpts.Permissions) || opt.contains(FillOpts.SdkURL)) {
        qFinder = qFinder.serviceAccountEnvironments.fetch()
      }

      if (opt.contains(FillOpts.ServiceAccountFilters)) {
        qFinder = qFinder.featureFilters.fetch()
      }

      if (!opt.contains(FillOpts.Archived)) {
        qFinder = qFinder.whenArchived.isNull
      }
    }
    return qFinder
  }

  @Throws(OptimisticLockingException::class)
  override fun update(portfolioId: UUID, personId: UUID, serviceAccount: ServiceAccount, appId: UUID?, opts: Opts): ServiceAccount? {
    val sa = QDbServiceAccount().id.eq(serviceAccount.id).whenArchived.isNull.portfolio.id.eq(portfolioId).findOne() ?: return null

    if (serviceAccount.version == null || serviceAccount.version != sa.version) {
      throw OptimisticLockingException()
    }

    val whoUpdated = convertUtils.byPerson(personId) ?: return null
    return update(sa, whoUpdated, serviceAccount, appId, opts, portfolioId)
  }

  @Throws(OptimisticLockingException::class)
  override fun update(
    serviceAccountId: UUID,
    updater: Person,
    serviceAccount: ServiceAccount,
    appId: UUID?,
    opts: Opts,
    portfolioId: UUID
  ): ServiceAccount? {
    val sa = QDbServiceAccount().id.eq(serviceAccountId).whenArchived.isNull.findOne() ?: return null

    if (serviceAccount.version == null || serviceAccount.version != sa.version) {
      throw OptimisticLockingException()
    }
    val whoUpdated = convertUtils.byPerson(updater) ?: return null
    return update(sa, whoUpdated, serviceAccount, appId, opts, portfolioId)
  }

  private fun update(
    sa: DbServiceAccount,
    whoUpdated: DbPerson,
    serviceAccount: ServiceAccount,
    appId: UUID?,
    opts: Opts,
    portfolioId: UUID
  ): ServiceAccount {
    val permChanges = ServiceAccountPermsUpdated()

    if (serviceAccount.permsInvalid == null || serviceAccount.permsInvalid == false) {
      permChanges.update(serviceAccount, appId, sa, convertUtils)
    }

    var descUpdated = false
    serviceAccount.description?.let {
      sa.description = it
      descUpdated = true
    }

    var updateAssociatedUser = false

    if (serviceAccount.name != sa.name) {
      sa.name = serviceAccount.name
      updateAssociatedUser = true
    }

    val filterUpdated = updateFilterOnServiceAccount(serviceAccount, sa, portfolioId)

    if (descUpdated || updateAssociatedUser || filterUpdated || permChanges.hasChanges()) {
      sa.whoChanged = whoUpdated
    }

    asyncUpdateCache(sa, updateServiceAccount(sa, permChanges).values)

    if (updateAssociatedUser) {
      internalPersonApi.updateSdkServiceAccountUser(sa.sdkPerson.id, whoUpdated, serviceAccount.name)
    }

    return convertUtils.toServiceAccount(sa, opts)!!
  }

  internal class ServiceAccountPermsUpdated {
    val deletePerms = mutableListOf<DbServiceAccountEnvironment>()
    val updatePerms = mutableListOf<DbServiceAccountEnvironment>()
    val createPerms = mutableListOf<DbServiceAccountEnvironment>()

    fun hasChanges() : Boolean {
      return deletePerms.isNotEmpty() || updatePerms.isNotEmpty() || createPerms.isNotEmpty()
    }

    val updated : List<DbServiceAccountEnvironment>
      get() = this.updatePerms
    val deleted : List<DbServiceAccountEnvironment>
      get() = this.deletePerms
    val created : List<DbServiceAccountEnvironment>
      get() = this.createPerms


    fun update(serviceAccount: ServiceAccount, appId: UUID?, sa: DbServiceAccount, convertUtils: Conversions) {
      val updatedEnvironments: MutableMap<UUID, ServiceAccountPermission> = HashMap()
      val newEnvironments: MutableList<UUID> = ArrayList()
      serviceAccount
        .permissions.forEach { perm: ServiceAccountPermission ->
          updatedEnvironments[perm.environmentId] = perm
          newEnvironments.add(perm.environmentId)
        }

      // we drop out of this knowing which perms to delete and update
      var finder = QDbServiceAccountEnvironment().environment.id
        .`in`(updatedEnvironments.keys).serviceAccount
        .eq(sa)

      // limit our area of interest
      if (appId != null) {
        finder = finder.environment.parentApplication.id.eq(appId)
      }

      finder
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

      var deleteFinder = QDbServiceAccountEnvironment().environment.id.notIn(updatedEnvironments.keys).serviceAccount.eq(sa)

      if (appId != null) {
        deleteFinder = deleteFinder.environment.parentApplication.id.eq(appId)
      }

      deleteFinder.findEach { toDelete ->
        deletePerms.add(toDelete)
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
    }

    private fun convertPermissionsToString(permissions: List<RoleType>): String {
      return permissions.toSet().joinToString(",") { rt ->
        rt.value
      }
    }
  }

  private fun updateFilterOnServiceAccount(serviceAccount: ServiceAccount, sa: DbServiceAccount, portfolioId: UUID): Boolean {
    // Update filter associations from the featureFilters list (null means "don't change", empty list clears)
    var filterUpdated = false

    serviceAccount.featureFilters?.let { updatedFilters ->
      // remove the records that we won't use any longer
      filterUpdated = sa.featureFilters.removeIf { !updatedFilters.contains(it.id) }

      val remainingIds = sa.featureFilters.map { it.id }

      updatedFilters.removeAll(remainingIds)

      if (updatedFilters.isNotEmpty()) {
        filterUpdated = true
        val toAdd = QDbFeatureFilter().id.`in`(updatedFilters).portfolio.id.eq(portfolioId).findList()
        sa.featureFilters.addAll(toAdd)
      }}

    return filterUpdated
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
        QDbAcl().roles.notEqualTo("")
          .environment.parentApplication.eq(application)
          .environment.whenUnpublished.isNull
          .environment.whenArchived.isNull
          .group.groupMembers.person.eq(person)
          .environment.fetch(QDbEnvironment.Alias.id)
          .findList()
      )
    }
    return qFinder.findList()
      .map { sa: DbServiceAccount? ->
        convertUtils.toServiceAccount(
          sa,
          opts,
          environmentPermissions
        )!!
      }
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

  @Transactional(type = TxType.REQUIRES_NEW)
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
          if (sa.apiKeyClientEval.isEmpty()) {
            updated = true
            sa.apiKeyClientEval = newClientEvalKey()
          }
          if (sa.apiKeyServerEval.isEmpty()) {
            updated = true
            sa.apiKeyServerEval = newServerEvalKey()
          }
          if (updated) {
            sa.update()
          }
        }
    }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
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
  @Transactional
  override fun create(
    portfolioId: UUID,
    creator: Person,
    serviceAccount: CreateServiceAccount,
    opts: Opts
  ): ServiceAccount? {
    val who = convertUtils.byPerson(creator) ?: return null
    val portfolio = convertUtils.byPortfolio(portfolioId) ?: return null

    val changedEnvironments = mutableListOf<DbEnvironment>()
    val envs = environmentMap(serviceAccount)

    // now where we actually find the environment, add it into the list
    val perms = serviceAccount.permissions.mapNotNull { sap: ServiceAccountPermission ->
      envs[sap.environmentId]?.let { e ->
        changedEnvironments.add(e)
        DbServiceAccountEnvironment.Builder()
          .environment(e)
          .permissions(convertPermissionsToString(sap.permissions))
          .build()
      }
    }.toMutableSet()

    val sdkPerson = internalPersonApi.createSdkServiceAccountUser(serviceAccount.name, who, false)
    // now create the SA and attach the perms to form the links
    val sa = DbServiceAccount(who, sdkPerson,
          serviceAccount.name, serviceAccount.description ?: "",
          newServerEvalKey(), newClientEvalKey(), portfolio).let {
      it.serviceAccountEnvironments = perms
      val filterIds = serviceAccount.featureFilter
      if (!filterIds.isNullOrEmpty()) {
        it.featureFilters = QDbFeatureFilter().id.`in`(filterIds).portfolio.id.eq(portfolioId).findList()
      }
      it
    }

    perms.forEach { p: DbServiceAccountEnvironment? -> p!!.serviceAccount = sa }

    try {
      save(sa)
      asyncUpdateCache(sa, changedEnvironments)
    } catch (dke: DuplicateKeyException) {
      log.warn("Duplicate service account {}", sa.name, dke)
      throw ServiceAccountApi.DuplicateServiceAccountException()
    }

    return convertUtils.toServiceAccount(sa, if (opts.contains(FillOpts.Permissions)) opts.add(FillOpts.SdkURL) else opts)
  }

  private fun environmentMap(serviceAccount: CreateServiceAccount): Map<UUID, DbEnvironment> {
    // find all the UUIDs in the environment list
    val envIds = serviceAccount.permissions.map { obj: ServiceAccountPermission -> obj.environmentId }

    // now find them in the db in one swoop using "in" syntax
    return QDbEnvironment().id.`in`(envIds).whenArchived.isNull.findList().associateBy { e -> e.id }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun save(sa: DbServiceAccount) {
    sa.save()
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun updateOnlyServiceAccount(sa: DbServiceAccount) {
    sa.update()
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun updateServiceAccount(
    sa: DbServiceAccount,
    permChanges: ServiceAccountPermsUpdated
  ) : MutableMap<UUID, DbEnvironment> {
    sa.markAsDirty() // ensure version is changed
    sa.update()
    permChanges.updated.forEach { it.update() }
    permChanges.deleted.forEach { it.delete() }
    permChanges.created.forEach { it.save() }
    val changed = mutableMapOf<UUID, DbEnvironment>()
    permChanges.deleted.forEach { e: DbServiceAccountEnvironment -> changed[e.environment.id] = e.environment }
    permChanges.updated.forEach { e: DbServiceAccountEnvironment -> changed[e.environment.id] = e.environment }
    permChanges.created.forEach { e: DbServiceAccountEnvironment -> changed[e.environment.id] = e.environment }
    changed.values.forEach { e ->
      e.markAsDirty()
      e.update()
    }
    return changed
  }

  // because this is an update or save, its no problem we send this out of band of this save/update.
  private fun asyncUpdateCache(
    sa: DbServiceAccount, changedEnvironments: Collection<DbEnvironment>?
  ) {
    cacheSource.updateServiceAccount(sa, PublishAction.UPDATE)
    changedEnvironments?.forEach { e ->
        cacheSource.updateEnvironment(
          e,
          PublishAction.UPDATE
        )
      }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  override fun delete(deleter: Person, serviceAccountId: UUID): Boolean {
    val sa = QDbServiceAccount().id.eq(serviceAccountId).whenArchived.isNull.findOne()
    if (sa != null) {
      sa.sdkPerson.let {
          internalPersonApi.deleteSdkServiceAccountUser(it.id, convertUtils.byPerson(deleter)!!)
      }

      archiveStrategy.archiveServiceAccount(sa)
      return true
    }
    return false
  }

  companion object {
    private val log = LoggerFactory.getLogger(ServiceAccountSqlApi::class.java)
  }

  /**
   * This is a transitional job that assumes that all the attached people to the sdkUser are invalid and created
   * new SDK style user accounts for them. After the migration that inserts the job, this is in fact TRUE.
   */
  @Transactional
  override fun ensure_service_accounts_have_person() {
    if (convertUtils.hasOrganisation()) {
      cleanupServiceAccountApiKeys()

      val superuserForOrganisation = mutableMapOf<UUID, DbPerson>()

      QDbServiceAccount().findList().forEach { sa ->
        val orgId = sa.portfolio.organization.id
        val superuser = superuserForOrganisation.computeIfAbsent(orgId) { id ->
          internalPersonApi.findSuperUserToBlame(id)
        }
        sa.sdkPerson = internalPersonApi.createSdkServiceAccountUser(sa.name, superuser, sa.whenArchived != null)
        sa.save()
      }
    }
  }

  // allow us to identify the user who created a feature change for instance
  override fun findServiceAccountByUserId(personId: UUID): UUID? {
    return QDbServiceAccount().sdkPerson.id.eq(personId).findOne()?.id
  }
}
