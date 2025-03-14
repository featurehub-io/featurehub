package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.*
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.db.utils.EnvironmentUtils
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@Singleton
class EnvironmentSqlApi @Inject constructor(
  private val database: Database,
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val archiveStrategy: ArchiveStrategy,
  private val internalFeatureApi: InternalFeatureApi,
  private val webhookEncryptionService: WebhookEncryptionService
) : EnvironmentApi {

  override fun portfolioEnvironmentBelongsTo(eId: UUID): UUID? {
    return QDbEnvironment()
      .select(QDbEnvironment.Alias.parentApplication.portfolio.id)
      .id.eq(eId).findOne()?.parentApplication?.portfolio?.id
  }

  /**
   * What roles does this person have in this environment?
   */
  override fun personRoles(current: Person, eid: UUID): EnvironmentRoles? {

    val environment = convertUtils.byEnvironment(eid, Opts.opts(FillOpts.Applications))
    val p = convertUtils.byPerson(current)
    val roles: MutableSet<RoleType> = HashSet()
    val appRoles: MutableSet<ApplicationRoleType> = HashSet()

    if (environment != null && p != null) {
      // is this person a portfolio admin? if so, they have all access to all environments in the portfolio
      if (convertUtils.isPersonEnvironmentAdmin(current, eid)) {
        return EnvironmentRoles.Builder()
          .applicationRoles(mutableSetOf(ApplicationRoleType.FEATURE_CREATE, ApplicationRoleType.FEATURE_EDIT_AND_DELETE))
          .environmentRoles(HashSet(listOf(*RoleType.values()))).build()
      }

      QDbAcl().environment.eq(environment).group.groupMembers.person.eq(p).findList().forEach { fe: DbAcl ->
        val splitRoles = convertUtils.splitEnvironmentRoles(fe.roles)
        roles.addAll(splitRoles)
        // as roles can have roles that are no READ and it makes no sense not to contain READ.
        if (roles.isNotEmpty() && !roles.contains(RoleType.READ)) {
          roles.add(RoleType.READ)
        }
      }

      QDbAcl().application.eq(environment.parentApplication).group.groupMembers.person.eq(p).findList()
        .forEach { fe: DbAcl ->
          appRoles.addAll(convertUtils.splitApplicationRoles(fe.roles))
        }
    }

    return EnvironmentRoles.Builder().applicationRoles(appRoles).environmentRoles(roles).build()
  }

  override fun delete(id: UUID?): Boolean {
    Conversions.nonNullEnvironmentId(id)
    val env = convertUtils.byEnvironment(id)
    if (env != null) {
      archiveStrategy.archiveEnvironment(env)

      QDbEnvironment().parentApplication.id.eq(env.parentApplication.id).priorEnvironment.id.eq(id).whenArchived.isNull.findList()
        .forEach { e ->
          e.priorEnvironment = env.priorEnvironment
          e.save()
        }
    }
    return env != null
  }

  override fun get(id: UUID, opts: Opts?, current: Person?): Environment? {
    Conversions.nonNullEnvironmentId(id)
    Conversions.nonNullPerson(current)
    val currentPerson = convertUtils.byPerson(current)
    if (currentPerson != null) {
      var env = QDbEnvironment().id.eq(id)
      if (convertUtils.personIsNotSuperAdmin(currentPerson)) {
        env = env.parentApplication.portfolio.groups.groupMembers.person.id.eq(currentPerson.id)
      }
      if (opts!!.contains(FillOpts.ServiceAccounts)) {
        env = env.serviceAccountEnvironments.fetch()
      }
      if (opts.contains(FillOpts.Features)) {
        env = env.environmentFeatures.fetch()
      }
      return env.findOneOrEmpty().map { e: DbEnvironment? ->
        convertUtils.toEnvironment(e, opts)
      }
        .orElse(null)
    }
    return null
  }

  override fun update(application: UUID, env: UpdateEnvironmentV2, opts: Opts): Environment? {
    val environment =
      QDbEnvironment().parentApplication.id.eq(application).id.eq(env.id).whenArchived.isNull.findOne() ?: return null

    if (env.name != null) {
      dupeEnvironmentNameCheck(env.name!!, environment)
    }

    circularPriorEnvironmentCheck(env.priorEnvironmentId, environment)

    if (env.description != null) {
      environment.description = env.description
    }

    if (env.production != null) {
      environment.isProductionEnvironment = java.lang.Boolean.TRUE == env.production
    }

    if (env.environmentInfo != null) {
      environment.userEnvironmentInfo =
        env.environmentInfo?.filter { !it.key.startsWith("mgmt.") }?.toMap()  // prevent mgmt prefixes being used
    }

    env.webhookEnvironmentInfo?.let { webhookEnvironmentInfo ->
      updateWebhookEnvironmentInfo(webhookEnvironmentInfo, environment)
    }

    update(environment)

    return convertUtils.toEnvironment(environment, opts)
  }

  @Throws(
    OptimisticLockingException::class,
    EnvironmentApi.DuplicateEnvironmentException::class,
    EnvironmentApi.InvalidEnvironmentChangeException::class
  )
  override fun update(envId: UUID, env: Environment, opts: Opts): Environment? {
    val environment = convertUtils.byEnvironment(envId) ?: return null

    if (environment.version != env.version) {
      throw OptimisticLockingException()
    }

    dupeEnvironmentNameCheck(env.name, environment)
    circularPriorEnvironmentCheck(env.priorEnvironmentId, environment)
    environment.description = env.description
    if (env.production != null) {
      environment.isProductionEnvironment = java.lang.Boolean.TRUE == env.production
    }
    update(environment)

    return convertUtils.toEnvironment(environment, opts)
  }

  override fun updateEnvironment(eid: UUID, env: UpdateEnvironment, opts: Opts): Environment? {
    val environment = convertUtils.byEnvironment(eid) ?: return null

    if (environment.version != env.version) {
      log.trace(
        "attempting to update old environment, current {}, update coming in is {}",
        environment.version,
        env.version
      )
      throw OptimisticLockingException()
    }

    if (env.name != null) {
      dupeEnvironmentNameCheck(env.name!!, environment)
    }

    circularPriorEnvironmentCheck(env.priorEnvironmentId, environment)

    if (env.description != null) {
      environment.description = env.description
    }

    if (env.production != null) {
      environment.isProductionEnvironment = java.lang.Boolean.TRUE == env.production
    }

    if (env.environmentInfo != null) {
      environment.userEnvironmentInfo =
        env.environmentInfo?.filter { !it.key.startsWith("mgmt.") }?.toMap()  // prevent mgmt prefixes being used
    }

    env.webhookEnvironmentInfo?.let { webhookEnvironmentInfo ->
      updateWebhookEnvironmentInfo(webhookEnvironmentInfo, environment)
    }

    update(environment)

    return convertUtils.toEnvironment(environment, opts)
  }

  private fun updateWebhookEnvironmentInfo(webhookEnvironmentInfo: Map<String,String>, environment: DbEnvironment) {
    val dbWebhookEnvironmentInfo = environment.webhookEnvironmentInfo ?: mutableMapOf()

    val updatedWebhookEnvironmentInfo = (dbWebhookEnvironmentInfo + webhookEnvironmentInfo).toMutableMap()
    val deleted = webhookEnvironmentInfo.filter { it.key.endsWith(".deleted") }
      .map { it.key.replace(".deleted", "") }

    deleted.forEach { deletedKey ->
      updatedWebhookEnvironmentInfo.remove(deletedKey)
      updatedWebhookEnvironmentInfo.remove("$deletedKey.deleted")
      updatedWebhookEnvironmentInfo.remove("$deletedKey.salt")
      updatedWebhookEnvironmentInfo.remove("$deletedKey.encrypted")
    }

    environment.webhookEnvironmentInfo = webhookEncryptionService.encrypt(updatedWebhookEnvironmentInfo.filter { !it.key.startsWith("mgmt.") }
      .toMap())
  }

  override fun getEnvironmentsUserCanAccess(appId: UUID, person: UUID): List<UUID>? {
    if (convertUtils.personIsSuperAdmin(person) || convertUtils.isPersonApplicationAdmin(person, appId)) return listOf()

    val envs = QDbEnvironment()
      .select(QDbEnvironment.Alias.id)
      .parentApplication.id.eq(appId).groupRolesAcl.group.groupMembers.person.id.eq(person).findList()

    return if (envs.isEmpty()) null else envs.map { it.id }
  }


  override fun migrateWebhookEnvInfo() {
    log.info("Migrating webhook environment info...")
    QDbEnvironment().findList().forEach { env ->
      if (env.userEnvironmentInfo != null) {
        val webhookInfo = env.userEnvironmentInfo
          .filter { it.key.startsWith("webhook.") && !it.key.contains(".headers") }
          .toMap()

        val webhookHeadersInfo = env.userEnvironmentInfo
          .filter { it.key.startsWith("webhook.") && it.key.contains(".headers") }
          .toMap()

        val webhookHeadersSplitMap = mutableMapOf<String, String>()
        webhookHeadersInfo.forEach { (key, value) ->
          val prefix = key.substringBeforeLast(".headers")
          val headers = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          headers.forEach {
            val keyValuePair = it.split("=")
            if (keyValuePair.size == 2 && keyValuePair[1].trim().isNotEmpty()) {
              webhookHeadersSplitMap["$prefix.headers.${keyValuePair[0]}"] = keyValuePair[1]
            }
          }
        }

        env.webhookEnvironmentInfo = webhookInfo + webhookHeadersSplitMap.toMap()

        env.userEnvironmentInfo = env.userEnvironmentInfo
          .filter { !it.key.startsWith("webhook.") }.toMap()

        env.save()
      }
    }
    log.info("Finished migrating webhook environment info.")
  }

  private fun circularPriorEnvironmentCheck(priorEnvironmentId: UUID?, environment: DbEnvironment) {
    // find anything that pointed to this environment and set it to what we used to point to
    val newPriorEnvironment = if (priorEnvironmentId == null) null else convertUtils.byEnvironment(
      priorEnvironmentId
    )
    if (newPriorEnvironment == null) {
      environment.priorEnvironment = null
    } else {
      // our purpose here is to make sure that if the newPriorEnvironment's tree as we walk up points to US (ennvironment)
      // then we have to point it to our old prior environment.
      val currentEnvOldPrior = environment.priorEnvironment
      environment.priorEnvironment = newPriorEnvironment
      var walk = newPriorEnvironment
      // so we walk up the NEW parent environment seeing if it points to us, and if so, point it to our old parent
      // make it null
      // otherwise walk up the tree until we can't walk anymore. Find any environments that
      // used to point to
      while (walk != null && walk.priorEnvironment != null) {
        val oldPrior = walk.priorEnvironment
        if (walk.priorEnvironment.id == environment.id) {
          walk.priorEnvironment = currentEnvOldPrior
          database.update(walk)
        }
        walk = oldPrior
      }
    }
  }

  @Throws(EnvironmentApi.DuplicateEnvironmentException::class)
  private fun dupeEnvironmentNameCheck(name: String, dbEnv: DbEnvironment) {
    val envName = name.trim { it <= ' ' }
    if (dbEnv.name != envName) {
      val dupe = QDbEnvironment().and().name.eq(
        envName
      ).parentApplication.eq(dbEnv.parentApplication).endAnd().findOne()
      if (dupe != null && dupe.id != dbEnv.id) {
        throw EnvironmentApi.DuplicateEnvironmentException()
      }
    }
    dbEnv.name = envName
  }

  // we assume
  // - person who created is a portfolio or superuser admin
  // - env has been validated for content
  @Throws(EnvironmentApi.DuplicateEnvironmentException::class, EnvironmentApi.InvalidEnvironmentChangeException::class)
  override fun create(env: CreateEnvironment, appId: UUID, whoCreated: Person): Environment? {
    val application = convertUtils.byApplication(appId) ?: return null
    val dbPerson = convertUtils.byPerson(whoCreated) ?: return null

    if (QDbEnvironment().and().name.eq(env.name).whenArchived.isNull.parentApplication.eq(application)
        .endAnd().exists()
    ) {
      throw EnvironmentApi.DuplicateEnvironmentException()
    }
    var priorEnvironment = convertUtils.byEnvironment(env.priorEnvironmentId)
    if ((priorEnvironment != null) && (priorEnvironment.parentApplication.id != application.id)) {
      throw EnvironmentApi.InvalidEnvironmentChangeException()
    }
    // so we don't have an environment so lets order them and put this one before the 1st one
    if (priorEnvironment == null) {
      val environments = QDbEnvironment().parentApplication.eq(application).whenArchived.isNull.findList()
      if (environments.isNotEmpty()) {
        promotionSortedEnvironments(environments)
        priorEnvironment = environments[environments.size - 1]
      }
    }
    val newEnv = DbEnvironment.Builder()
      .description(env.description)
      .name(env.name)
      .priorEnvironment(priorEnvironment)
      .userEnvironmentInfo(env.environmentInfo?.filter { !it.key.startsWith("mgmt.") }
        ?.toMap())  // prevent mgmt prefixes being used
      .parentApplication(application)
      .productionEnvironment(java.lang.Boolean.TRUE == env.production)
      .build()
    val createdEnvironment = update(newEnv)
    discoverMissingBooleanApplicationFeaturesForThisEnvironment(createdEnvironment, dbPerson)
    return convertUtils.toEnvironment(createdEnvironment, Opts.empty())
  }

  private fun discoverMissingBooleanApplicationFeaturesForThisEnvironment(
    createdEnvironment: DbEnvironment,
    whoCreated: DbPerson
  ) {

    for (nf in saveAllFeatures(createdEnvironment, whoCreated)) {
      cacheSource.publishFeatureChange(nf)
    }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun saveAllFeatures(createdEnvironment: DbEnvironment,
                              whoCreated: DbPerson): List<DbFeatureValue> {
    val newFeatures =
      QDbApplicationFeature().whenArchived.isNull.parentApplication.eq(createdEnvironment.parentApplication).valueType.eq(
        FeatureValueType.BOOLEAN
      ).findList()
        .map { af ->
          val fv = DbFeatureValue(whoCreated, true, af, createdEnvironment, false.toString())
          internalFeatureApi.saveFeatureValue(fv)
          fv
        }

    return newFeatures
  }

  private fun promotionSortedEnvironments(environments: List<DbEnvironment>?) {
    EnvironmentUtils.sortEnvironments(environments)
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun update(env: DbEnvironment): DbEnvironment {
    env.markAsDirty()
    env.save()
    cacheSource.updateEnvironment(env, PublishAction.UPDATE)
    return env
  }

  override fun search(
    appId: UUID?,
    filter: String?,
    order: EnvironmentSortOrder?,
    opts: Opts?,
    current: Person?
  ): List<Environment> {
    Conversions.nonNullApplicationId(appId)

    val application = convertUtils.byApplication(appId)

    if (application != null) {
      val currentPerson = convertUtils.byPerson(current)
      if (currentPerson != null) {
        var eq = QDbEnvironment().parentApplication.eq(application)
        if (filter != null) {
          eq = eq.name.ilike("%$filter%")
        }
        eq = fetchEnvironmentOpts(opts, eq)
        if (EnvironmentSortOrder.ASC == order) {
          eq = eq.order().name.asc()
        } else if (EnvironmentSortOrder.DESC == order) {
          eq = eq.order().name.desc()
        }
        if (!opts!!.contains(FillOpts.Archived)) {
          eq = eq.whenArchived.isNull
        }
        if (convertUtils.personIsNotSuperAdmin(currentPerson)) {
          eq = eq.parentApplication.portfolio.groups.groupMembers.person.id.eq(currentPerson.id)
        }

        val environmentList = eq.findList().toMutableList()

        if (order == null || order == EnvironmentSortOrder.PRIORITY) {
          EnvironmentUtils.sortEnvironments(environmentList)
        }

        return environmentList.map { e: DbEnvironment? -> convertUtils.toEnvironment(e, opts)!! }
      }
    }

    return listOf()
  }

  override fun findPortfolio(envId: UUID?): Portfolio? {
    return if (envId != null) {
      convertUtils.toPortfolio(
        QDbPortfolio().applications.environments.id.eq(envId).findOne(), Opts.empty()
      )
    } else null
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  override fun unpublishEnvironments(appId: UUID, environments: List<UUID>?): Int {
    var envQ = QDbEnvironment().parentApplication.id.eq(appId)

    if (environments != null && environments.isNotEmpty()) {
      envQ = envQ.id.`in`(environments)
    }

    envQ.select(QDbEnvironment.Alias.id).findList().forEach { env ->
      cacheSource.deleteEnvironment(env.id)
    }

    return envQ.asUpdate().set("whenUnpublished", Instant.now()).update()
  }

  private fun fetchEnvironmentOpts(opts: Opts?, eq: QDbEnvironment): QDbEnvironment {
    var query = eq
    if (opts!!.contains(FillOpts.Acls)) {
      query = query.groupRolesAcl.fetch()
    }
    return query
  }

  override fun setOrdering(app: Application, environments: List<Environment>): List<Environment>? {
    val envFinder = QDbEnvironment().parentApplication.id.eq(app.id).whenArchived.isNull
    val envs = envFinder.findList().associateBy { it.id }

    for (e in environments) {
      val dbEnv = envs[e.id]
      if (dbEnv == null) {
        log.error("Environment {} not found", e.id)
        return null
      }
      if (dbEnv.version != e.version) {
        log.error("Environment version should be {} and is {}", dbEnv.version, e.version)
        return null
      }
      if (e.priorEnvironmentId != null && envs[e.priorEnvironmentId] == null) {
        log.error("Attempted to set a prior environment id you didn't pass")
        return null
      }
    }
    if (environments.size > 1) {
      val destinations = environments.associateBy { it.id }
      for (e in environments) {
        // create a slot for each environment
        val spot: MutableMap<UUID, Int> = environments.map { env -> env.id to 0 }.toMap() as MutableMap<UUID, Int>

        // set our one to "visited"
        spot[e.id] = 1
        // now walk backwards until we either hit the end or see "visited"
        var currentId = e.priorEnvironmentId
        while (currentId != null && spot[currentId] == 0) {
          spot[currentId] = 1
          currentId = destinations[currentId]!!.priorEnvironmentId
        }
        if (currentId != null) {
          log.error("circular environment in {}", currentId)
          return null
        }
      }

      // ok they all seem to be ok
      updatePriorEnvironmentIds(envs, environments)
    }

    val emptyOpts = Opts.opts()
    return envFinder.findList().map { e: DbEnvironment -> convertUtils.toEnvironment(e, emptyOpts)!! }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun updatePriorEnvironmentIds(envs: Map<UUID?, DbEnvironment?>, environments: List<Environment>) {
    for (e in environments) {
      val env = envs[e.id]
      val prior = if (e.priorEnvironmentId == null) null else envs[e.priorEnvironmentId]
      env!!.priorEnvironment = prior
      database.save(env)
    }
  }

  fun getEnvironment(appId: UUID, envName: String): Environment? {
    return QDbEnvironment().parentApplication.id.eq(appId).name.ieq(envName).findOne()
      ?.let { convertUtils.toEnvironment(it, Opts.empty()) }
  }

  companion object {
    private val log = LoggerFactory.getLogger(EnvironmentSqlApi::class.java)
  }
}
