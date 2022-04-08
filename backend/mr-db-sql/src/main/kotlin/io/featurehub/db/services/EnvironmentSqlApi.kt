package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.*
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.db.publish.CacheSource
import io.featurehub.db.utils.EnvironmentUtils
import io.featurehub.mr.model.*
import io.featurehub.mr.model.RoleType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

@Singleton
class EnvironmentSqlApi @Inject constructor(
  private val database: Database,
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val archiveStrategy: ArchiveStrategy
) : EnvironmentApi {
  override fun personRoles(current: Person?, eid: UUID?): EnvironmentRoles? {
    Conversions.nonNullPerson(current)
    Conversions.nonNullEnvironmentId(eid)
    val environment = convertUtils.byEnvironment(eid, Opts.opts(FillOpts.Applications))
    val p = convertUtils.byPerson(current)
    val roles: MutableSet<RoleType> = HashSet()
    val appRoles: MutableSet<ApplicationRoleType> = HashSet()
    if (environment != null && p != null) {
      // is this person a portfolio admin? if so, they have all access to all environments in the portfolio
      if (QDbGroup().adminGroup.isTrue.whenArchived.isNull.groupMembers.person.eq(p).owningPortfolio.applications.environments.eq(
          environment
        ).exists()
      ) {
        return EnvironmentRoles.Builder()
          .applicationRoles(HashSet(listOf(*ApplicationRoleType.values())))
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
      QDbAcl().application.eq(environment.parentApplication).group.groupMembers.person.eq(p).findList().forEach { fe: DbAcl ->
        val splitRoles = convertUtils.splitApplicationRoles(fe.roles)
        if (splitRoles != null && splitRoles.contains(ApplicationRoleType.FEATURE_EDIT)) {
          appRoles.add(ApplicationRoleType.FEATURE_EDIT)
        }
      }
    }
    return EnvironmentRoles.Builder().applicationRoles(appRoles).environmentRoles(roles).build()
  }

  override fun delete(id: UUID?): Boolean {
    Conversions.nonNullEnvironmentId(id)
    val env = convertUtils.byEnvironment(id)
    if (env != null) {
      archiveStrategy.archiveEnvironment(env)

      QDbEnvironment().parentApplication.id.eq(env.parentApplication.id).priorEnvironment.id.eq(id).whenArchived.isNull.findList().forEach { e ->
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
      return env.findOneOrEmpty().map { e: DbEnvironment? -> convertUtils.toEnvironment(e, opts) }
        .orElse(null)
    }
    return null
  }

  @Transactional
  @Throws(
    OptimisticLockingException::class,
    EnvironmentApi.DuplicateEnvironmentException::class,
    EnvironmentApi.InvalidEnvironmentChangeException::class
  )
  override fun update(envId: UUID?, env: Environment?, opts: Opts?): Environment? {
    Conversions.nonNullEnvironmentId(envId)
    val environment = convertUtils.byEnvironment(envId)
    if (environment != null) {
      if (env!!.version == null || environment.version != env.version) {
        throw OptimisticLockingException()
      }
      dupeEnvironmentNameCheck(env, environment)
      circularPriorEnvironmentCheck(env, environment)
      environment.description = env.description
      if (env.production != null) {
        environment.isProductionEnvironment = java.lang.Boolean.TRUE == env.production
      }
      update(environment)
      return convertUtils.toEnvironment(environment, opts)
    }
    return null
  }

  private fun circularPriorEnvironmentCheck(env: Environment?, environment: DbEnvironment) {
    // find anything that pointed to this environment and set it to what we used to point to
    val newPriorEnvironment = if (env!!.priorEnvironmentId == null) null else convertUtils.byEnvironment(
      env.priorEnvironmentId
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
  private fun dupeEnvironmentNameCheck(env: Environment?, dbEnv: DbEnvironment) {
    if (env?.name != null) {
      env.name = env.name.trim { it <= ' ' }
      if (dbEnv.name != env.name) {
        val dupe = QDbEnvironment().and().name.eq(
          env.name
        ).parentApplication.eq(dbEnv.parentApplication).endAnd().findOne()
        if (dupe != null && dupe.id != dbEnv.id) {
          throw EnvironmentApi.DuplicateEnvironmentException()
        }
      }
      dbEnv.name = env.name
    }
  }

  // we assume
  // - person who created is a portfolio or superuser admin
  // - env has been validated for content
  @Throws(EnvironmentApi.DuplicateEnvironmentException::class, EnvironmentApi.InvalidEnvironmentChangeException::class)
  override fun create(env: Environment?, app: Application?, whoCreated: Person?): Environment? {
    val application = convertUtils.byApplication(app!!.id)
    if (application != null) {
      if (QDbEnvironment().and().name.eq(env!!.name).whenArchived.isNull.parentApplication.eq(application)
          .endAnd().exists()
      ) {
        throw EnvironmentApi.DuplicateEnvironmentException()
      }
      var priorEnvironment = convertUtils.byEnvironment(env.priorEnvironmentId)
      if (priorEnvironment != null && priorEnvironment.parentApplication.id != application.id) {
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
        .parentApplication(application)
        .productionEnvironment(java.lang.Boolean.TRUE == env.production)
        .build()
      val createdEnvironment = update(newEnv)
      cacheSource.updateEnvironment(createdEnvironment, PublishAction.CREATE)
      discoverMissingBooleanApplicationFeaturesForThisEnvironment(createdEnvironment, whoCreated)
      return convertUtils.toEnvironment(createdEnvironment, Opts.empty())
    }
    return null
  }

  private fun discoverMissingBooleanApplicationFeaturesForThisEnvironment(
    createdEnvironment: DbEnvironment,
    whoCreated: Person?
  ) {
    val newFeatures =
      QDbApplicationFeature().whenArchived.isNull.parentApplication.eq(createdEnvironment.parentApplication).valueType.eq(
        FeatureValueType.BOOLEAN
      ).findList().stream()
        .map { af: DbApplicationFeature? ->
          DbFeatureValue.Builder()
            .defaultValue(java.lang.Boolean.FALSE.toString())
            .environment(createdEnvironment)
            .feature(af)
            .featureState(FeatureState.ENABLED)
            .locked(true)
            .whoUpdated(convertUtils.byPerson(whoCreated))
            .build()
        }.collect(Collectors.toList())
    saveAllFeatures(newFeatures)
    for (nf in newFeatures) {
      cacheSource.publishFeatureChange(nf)
    }
  }

  @Transactional
  private fun saveAllFeatures(newFeatures: List<DbFeatureValue>) {
    newFeatures.forEach { bean: DbFeatureValue? -> database.save(bean) }
  }

  private fun promotionSortedEnvironments(environments: List<DbEnvironment>?) {
    EnvironmentUtils.sortEnvironments(environments)
  }

  @Transactional
  private fun update(env: DbEnvironment): DbEnvironment {
    database.save(env)
    cacheSource.updateEnvironment(env, PublishAction.UPDATE)
    return env
  }

  override fun search(
    appId: UUID?,
    filter: String?,
    order: SortOrder?,
    opts: Opts?,
    current: Person?
  ): List<Environment?>? {
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
        if (SortOrder.ASC == order) {
          eq = eq.order().name.asc()
        } else if (SortOrder.DESC == order) {
          eq = eq.order().name.desc()
        }
        if (!opts!!.contains(FillOpts.Archived)) {
          eq = eq.whenArchived.isNull
        }
        if (convertUtils.personIsNotSuperAdmin(currentPerson)) {
          eq = eq.parentApplication.portfolio.groups.groupMembers.person.id.eq(currentPerson.id)
        }
        return eq.findList().stream().map { e: DbEnvironment? -> convertUtils.toEnvironment(e, opts) }
          .collect(Collectors.toList())
      }
    }
    return ArrayList()
  }

  override fun findPortfolio(envId: UUID?): Portfolio? {
    return if (envId != null) {
      convertUtils.toPortfolio(
        QDbPortfolio().applications.environments.id.eq(envId).findOne(), Opts.empty()
      )
    } else null
  }

  @Transactional
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
      val destinations = environments.stream().collect(
        Collectors.toMap(
          { obj: Environment -> obj.id },
          java.util.function.Function.identity()
        )
      )
      for (e in environments) {
        // create a slot for each environment
        val spot: MutableMap<UUID, Int> = environments.map { env -> env.id!! to 0 }.toMap() as MutableMap<UUID, Int>

        // set our one to "visited"
        spot[e.id!!] = 1
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
    return envFinder.findList().map { e: DbEnvironment? -> convertUtils.toEnvironment(e, emptyOpts) }
  }

  @Transactional
  private fun updatePriorEnvironmentIds(envs: Map<UUID?, DbEnvironment?>, environments: List<Environment>) {
    for (e in environments) {
      val env = envs[e.id]
      val prior = if (e.priorEnvironmentId == null) null else envs[e.priorEnvironmentId]
      env!!.priorEnvironment = prior
      database.save(env)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(EnvironmentSqlApi::class.java)
  }
}
