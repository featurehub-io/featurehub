package io.featurehub.db.services

import io.ebean.Database
import io.ebean.annotation.Transactional
import io.featurehub.db.api.*
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.model.DbAcl
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.query.QDbAcl
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.publish.CacheSource
import io.featurehub.db.services.strategies.StrategyDiffer
import io.featurehub.db.utils.EnvironmentUtils
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class FeatureSqlApi @Inject constructor(
    private val database: Database, private val convertUtils: Conversions, private val cacheSource: CacheSource,
    private val rolloutStrategyValidator: RolloutStrategyValidator, private val strategyDiffer: StrategyDiffer
) : FeatureApi, FeatureUpdateBySDKApi {
  @Throws(OptimisticLockingException::class, RolloutStrategyValidator.InvalidStrategyCombination::class, FeatureApi.NoAppropriateRole::class)
  override fun createFeatureValueForEnvironment(
      eId: UUID,
      key: String,
      featureValue: FeatureValue,
      person: PersonFeaturePermission
  ): FeatureValue {
    if (!person.hasWriteRole()) {
      val env = QDbEnvironment().id.eq(eId).whenArchived.isNull.findOne()
      log.warn("User has no roles for environment {} key {}", eId, key)
      if (env == null) {
        log.error("could not find environment or environment is archived")
      } else {
        log.error(
          "error env {} app {} portfolio {}",
          env.name,
          env.parentApplication.name,
          env.parentApplication.portfolio.name
        )
      }
      throw FeatureApi.NoAppropriateRole()
    }

    rolloutStrategyValidator.validateStrategies(
      featureValue.rolloutStrategies,
      featureValue.rolloutStrategyInstances
    ).hasFailedValidation()

    val dbFeatureValue = QDbFeatureValue().environment.id.eq(eId).feature.key.eq(key).findOne()

    return if (dbFeatureValue != null) {
      // this is an update not a create, environment + app-feature key exists
      onlyUpdateFeatureValueForEnvironment(featureValue, person, dbFeatureValue)!!
    } else if (person.hasChangeValueRole() || person.hasLockRole() || person.hasUnlockRole()) {
      onlyCreateFeatureValueForEnvironment(eId, key, featureValue, person)!!
    } else {
      log.info(
        "roles for person are {} and are not enough for environment {} and key {}",
        person.toString(),
        eId,
        key
      )
      throw FeatureApi.NoAppropriateRole()
    }
  }

  @Throws(FeatureApi.NoAppropriateRole::class)
  private fun onlyCreateFeatureValueForEnvironment(
      eid: UUID, key: String, featureValue: FeatureValue?,
      person: PersonFeaturePermission
  ): FeatureValue? {
    val environ = convertUtils.byEnvironment(eid)
    if (environ != null) {
      val appFeature = QDbApplicationFeature().key.eq(key).parentApplication.environments.eq(environ).findOne()
      if (appFeature != null) {
        if (strategyDiffer.invalidStrategyInstances(featureValue!!.rolloutStrategyInstances, appFeature)) {
          log.error("Invalid rollout strategy instances")
          return null
        }
        val dbFeatureValue = DbFeatureValue.Builder()
          .environment(environ)
          .feature(appFeature)
          .build()
        updateFeatureValue(featureValue, person, dbFeatureValue)
        save(dbFeatureValue)
        publish(dbFeatureValue)
        return convertUtils.toFeatureValue(dbFeatureValue)
      } else {
        log.error(
          "Attempted to create feature value in environment `{}` where feature key did not exist: `{}`",
          eid,
          key
        )
      }
    }
    return null
  }

  @Transactional
  private fun save(featureValue: DbFeatureValue) {
    database.save(featureValue)
  }

  private fun publish(featureValue: DbFeatureValue) {
    log.trace("publishing update for {}", featureValue)
    cacheSource.publishFeatureChange(featureValue)
  }

  @Transactional
  override fun deleteFeatureValueForEnvironment(eId: UUID, key: String): Boolean {
      Conversions.nonNullEnvironmentId(eId)
    val strategy = QDbFeatureValue().environment.id.eq(eId).feature.key.eq(key).findOne()
    if (strategy != null) {
      cacheSource.deleteFeatureChange(strategy.feature, strategy.environment.id)
      return database.delete(strategy)
    }
    return false
  }

  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class)
  private fun onlyUpdateFeatureValueForEnvironment(
      featureValue: FeatureValue,
      person: PersonFeaturePermission,
      strategy: DbFeatureValue
  ): FeatureValue? {
    if (featureValue.version == null || strategy.version != featureValue.version) {
      throw OptimisticLockingException()
    }
    if (strategyDiffer.invalidStrategyInstances(featureValue.rolloutStrategyInstances, strategy.feature)) {
      log.error("Invalid rollout strategy instances")
      return null
    }
    updateFeatureValue(featureValue, person, strategy)
    save(strategy)
    publish(strategy)
    return convertUtils.toFeatureValue(strategy)
  }

  @Throws(FeatureApi.NoAppropriateRole::class)
  private fun updateFeatureValue(
      featureValue: FeatureValue,
      person: PersonFeaturePermission,
      dbFeatureValue: DbFeatureValue
  ) {
    val feature = dbFeatureValue.feature
    if (person.hasChangeValueRole() && (!dbFeatureValue.isLocked || java.lang.Boolean.FALSE == featureValue.locked && person.hasUnlockRole())) {
      if (feature.valueType == FeatureValueType.NUMBER) {
        dbFeatureValue.defaultValue =
          if (featureValue.valueNumber == null) null else featureValue.valueNumber.toString()
      } else if (feature.valueType == FeatureValueType.STRING) {
        dbFeatureValue.defaultValue = featureValue.valueString
      } else if (feature.valueType == FeatureValueType.JSON) {
        dbFeatureValue.defaultValue = featureValue.valueJson
      } else if (feature.valueType == FeatureValueType.BOOLEAN) {
        dbFeatureValue.defaultValue =
          if (featureValue.valueBoolean == null) java.lang.Boolean.FALSE.toString() else featureValue.valueBoolean.toString()
      }
      if (featureValue.rolloutStrategies != null) {
        featureValue.rolloutStrategies!!.forEach { rs: RolloutStrategy ->
          if (rs.id == null) {
            rs.id = UUID.randomUUID().toString()
          }
        }
      }
      dbFeatureValue.rolloutStrategies = featureValue.rolloutStrategies
      strategyDiffer.createDiff(featureValue, dbFeatureValue)
    }

    // change locked before changing value, as may not be able to change value if locked
    val newValue = featureValue.locked
    if (newValue != dbFeatureValue.isLocked) {
      if (!newValue && person.hasUnlockRole()) {
        dbFeatureValue.isLocked = false
      } else if (newValue && person.hasLockRole()) {
        dbFeatureValue.isLocked = true
      } else {
        throw FeatureApi.NoAppropriateRole()
      }
    }

    dbFeatureValue.whoUpdated = convertUtils.byPerson(person.person)

    if (dbFeatureValue.whoUpdated == null) {
      log.error("Unable to set who updated on dbFeatureValue {}", person.person)
    }

    if (person.hasChangeValueRole()) {
      dbFeatureValue.retired = featureValue.retired
    }
  }

  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class, RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateFeatureValueForEnvironment(
      eid: UUID,
      key: String,
      featureValue: FeatureValue,
      person: PersonFeaturePermission
  ): FeatureValue {
    return createFeatureValueForEnvironment(eid, key, featureValue, person)
  }

  override fun getFeatureValueForEnvironment(eid: UUID, key: String): FeatureValue? {
      Conversions.nonNullEnvironmentId(eid)
    val strategy =
      QDbFeatureValue().environment.id.eq(eid).feature.key.eq(key).sharedRolloutStrategies.fetch().findOne()
    return if (strategy == null) null else convertUtils.toFeatureValue(strategy)
  }

  override fun getAllFeatureValuesForEnvironment(eId: UUID): EnvironmentFeaturesResult {
      Conversions.nonNullEnvironmentId(eId)
    return EnvironmentFeaturesResult()
      .featureValues(
        QDbFeatureValue().environment.id.eq(eId).feature.whenArchived.isNull.findList().stream()
          .map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs) }
          .collect(Collectors.toList()))
      .environments(listOf(convertUtils.toEnvironment(QDbEnvironment().id.eq(eId).findOne(), Opts.empty())))
  }

  // we are going to have to put a transaction at this level as we want the whole thing to roll back if there is an issue
  @Transactional
  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class, RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateAllFeatureValuesForEnvironment(
      eId: UUID,
      featureValues: List<FeatureValue>,
      person: PersonFeaturePermission
  ): List<FeatureValue> {
      Conversions.nonNullEnvironmentId(eId)
    require(
      !(featureValues.size != featureValues.stream().map { obj: FeatureValue -> obj.key }
        .collect(Collectors.toSet()).size)) { "Invalid update dataset" }

    // ensure the strategies are valid from a conceptual perspective
    val failure = RolloutStrategyValidator.ValidationFailure()
    for (fv in featureValues) {
      rolloutStrategyValidator.validateStrategies(fv.rolloutStrategies, fv.rolloutStrategyInstances, failure)
    }
    failure.hasFailedValidation()
    val existing = QDbFeatureValue().environment.id.eq(eId).feature.whenArchived.isNull.findList()
    val newValues = featureValues.associateBy({ it.key }, { it }).toMutableMap()

    // take them all and remove all fv's we were passed, leaving only EFS's we want to remove
    val deleteKeys = existing.stream().map { e: DbFeatureValue -> e.feature.key }
      .collect(Collectors.toSet())
    for (fv in featureValues) {
      deleteKeys.remove(fv.key)
    }

    // we should be left with only keys in deleteKeys that do not exist in the passed in list of feature values
    // and in addingKeys we should be given a list of keys which exist in the passed in FV's but didn't exist in the db
    val deleteStrategies: MutableList<DbFeatureValue> = ArrayList()
    for (strategy in existing) {
      if (deleteKeys.contains(strategy.feature.key)) {
        if (strategy.feature.valueType != FeatureValueType.BOOLEAN) {
          deleteStrategies.add(strategy) // can't delete booleans
        }
      } else {
        val fv = newValues.remove(strategy.feature.key)
        if (fv != null) {
          onlyUpdateFeatureValueForEnvironment(fv, person, strategy)
        }
      }
    }

    // now for the creates
    for (key in newValues.keys) {
      val fv = newValues[key]
      onlyCreateFeatureValueForEnvironment(eId, key, fv, person)
    }
    if (deleteStrategies.isNotEmpty()) {
      publishTheRemovalOfABunchOfStrategies(deleteStrategies)
      database.deleteAll(deleteStrategies)
    }
    return QDbFeatureValue().environment.id.eq(eId).feature.whenArchived.isNull.findList().stream()
      .map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs) }
      .collect(Collectors.toList())
  }

  // can't background this because they will deleted shortly
  private fun publishTheRemovalOfABunchOfStrategies(deleteStrategies: Collection<DbFeatureValue?>) {
    if (!deleteStrategies.isEmpty()) {
      deleteStrategies.parallelStream().forEach { strategy: DbFeatureValue? ->
        cacheSource.deleteFeatureChange(
          strategy!!.feature, strategy.environment.id
        )
      }
    }
  }

  @Throws(RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateFeature(
      sdkUrl: String, eid: UUID, featureKey: String, updatingValue: Boolean,
      buildFeatureValue: Function<FeatureValueType, FeatureValue>
  ) {
      Conversions.nonNullEnvironmentId(eid)

    // not checking permissions, edge checks those
    val feature = QDbApplicationFeature().parentApplication.environments.id.eq(eid).key.eq(featureKey)
      .findOne()
      ?: return
    var fv = QDbFeatureValue().environment.id.eq(eid).feature.eq(feature).findOne()
    val newValue = buildFeatureValue.apply(feature.valueType)
    rolloutStrategyValidator.validateStrategies(
      newValue.rolloutStrategies,
      newValue.rolloutStrategyInstances
    ).hasFailedValidation()
    val saveNew = fv == null
    if (saveNew) { // creating
      fv = DbFeatureValue.Builder()
        .environment(QDbEnvironment().id.eq(eid).findOne())
        .feature(feature)
        .locked(true)
        .build()
    }
    if (updatingValue) {
      when (fv!!.feature.valueType) {
        FeatureValueType.BOOLEAN -> fv.defaultValue =
          if (newValue.valueBoolean == null) java.lang.Boolean.FALSE.toString() else newValue.valueBoolean.toString()
        FeatureValueType.STRING -> fv.defaultValue = newValue.valueString
        FeatureValueType.NUMBER -> fv.defaultValue =
          if (newValue.valueNumber == null) null else newValue.valueNumber.toString()
        FeatureValueType.JSON -> fv.defaultValue = newValue.valueJson
      }
    }
    fv!!.isLocked = newValue.locked

    // API can never change strategies
    save(fv)
    publish(fv)
  }

  internal class EnvironmentsAndStrategies(
      var featureValues: MutableMap<UUID, DbFeatureValue>,
      var roles: Map<UUID, List<RoleType>>,
      var environments: Map<UUID, DbEnvironment>,
      var appRolesForThisPerson: Set<ApplicationRoleType>
  )

  private fun strategiesUserCanAccess(appId: UUID, key: String, person: Person): EnvironmentsAndStrategies? {
    val dbPerson = convertUtils.byPerson(person)
    val app = convertUtils.byApplication(appId)
    if (app == null || app.whenArchived != null || dbPerson == null || dbPerson.whenArchived != null) {
      return null
    }
    val strategiesResult: MutableMap<UUID, DbFeatureValue> = mutableMapOf()
    val roles: MutableMap<UUID, List<RoleType>> = mutableMapOf()
    val environments: MutableMap<UUID, DbEnvironment> = mutableMapOf()
    val personAdmin = convertUtils.isPersonApplicationAdmin(dbPerson, app)
    val strategies = QDbFeatureValue().feature.key.eq(key).feature.parentApplication
      .eq(app)
      .findList()
      .associateBy( {it.environment.id}, {it}).toMutableMap()

    val adminRoles = listOf(*RoleType.values())
    if (!personAdmin) { // is they aren't a portfolio admin, figure out what their permissions are to each environment
      QDbAcl().environment.parentApplication.eq(app).group.peopleInGroup.eq(dbPerson).findList().forEach { fe: DbAcl ->
          log.debug(
            "Found environment `{}`, app `{}`, group `{}`, roles `{}`",
            if (fe.environment == null) "<none>" else fe.environment.name,
            if (fe.application == null) "<none>" else fe.application.name,
            fe.group.name, fe.roles
          )
          val roleTypes = convertUtils.splitEnvironmentRoles(fe.roles)
          if (roleTypes.isNotEmpty()) {
            val envId = fe.environment.id
            roles[envId] = roleTypes
            environments[envId] = fe.environment
            val strategy = strategies.remove(envId)
            if (strategy != null) {
              strategiesResult[envId] = strategy
            }
          }
        }
    }

    // they have at least one environment or they are an admin
    if (environments.isNotEmpty() || personAdmin) {
      val emptyRoles = emptyList<RoleType>()
      QDbEnvironment().parentApplication.eq(app).findList().forEach { env: DbEnvironment ->
        if (environments[env.id] == null) {
          environments[env.id] = env
          roles[env.id] = if (personAdmin) adminRoles else emptyRoles
          if (personAdmin) {
            val strategy = strategies.remove(env.id)
            if (strategy != null) {
              strategiesResult[env.id] = strategy
            }
          }
        }
      }
    }
    val appRoles = QDbAcl().application.isNotNull
      .select(QDbAcl.Alias.roles).roles.isNotNull.group.whenArchived.isNull.group.owningPortfolio.eq(app.portfolio).group.peopleInGroup.eq(
        dbPerson
      ).findList().stream()
      .map { appAcl: DbAcl -> convertUtils.splitApplicationRoles(appAcl.roles) }
      .flatMap { obj: List<ApplicationRoleType> -> obj.stream() }.collect(Collectors.toSet())

    return EnvironmentsAndStrategies(strategiesResult, roles, environments, appRoles)
  }

  override fun getFeatureValuesForApplicationForKeyForPerson(
      appId: UUID,
      key: String,
      person: Person
  ): List<FeatureEnvironment> {
      Conversions.nonNullApplicationId(appId)
    val result = strategiesUserCanAccess(appId, key, person) ?: return emptyList()

    return result.environments.keys.map { e ->
      convertUtils.toFeatureEnvironment(result.featureValues[e], result.roles[e]!!,
        result.environments[e]!!, Opts.opts(FillOpts.ServiceAccounts)
      )
    }
  }

  @Transactional
  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class, RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateAllFeatureValuesByApplicationForKey(
      id: UUID,
      key: String,
      featureValue: List<FeatureValue>,
      person: Person,
      removeValuesNotPassed: Boolean
  ) {
    val failure = RolloutStrategyValidator.ValidationFailure()

    for (fv in featureValue) {
      rolloutStrategyValidator.validateStrategies(fv.rolloutStrategies, fv.rolloutStrategyInstances, failure)
    }
    failure.hasFailedValidation()
    val result = strategiesUserCanAccess(id, key, person)
    if (result != null) {
      // environment id -> role in that environment
      val environmentToRoleMap = result.roles
      // environment -> feature value
      val strategiesToDelete = result.featureValues
      for (fv in featureValue) {
        val envId = fv.environmentId
        if (envId == null) {
          log.warn("Trying to update for environment `{}` and environment id is invalid.", fv.environmentId)
          throw FeatureApi.NoAppropriateRole()
        }
        val roles = environmentToRoleMap[envId]
        if (roles == null) {
          log.warn(
            "Trying to update for environment `{}` and environment id has no roles (no permissions).",
            envId
          )
          throw FeatureApi.NoAppropriateRole()
        }
        createFeatureValueForEnvironment(
          fv.environmentId!!, key, fv,
          PersonFeaturePermission.Builder()
            .person(person)
            .appRoles(result.appRolesForThisPerson)
            .roles(HashSet(roles)).build()
        )
        strategiesToDelete.remove(envId) // we processed this environment ok, didn't throw a wobbly
      }

      // now remove any ability to remove feature values that are flags
      val invalidDeletions = strategiesToDelete.keys.stream()
        .filter { u: UUID -> strategiesToDelete[u]!!.feature.valueType != FeatureValueType.BOOLEAN }
        .collect(Collectors.toList())

      invalidDeletions.forEach { strategiesToDelete.remove(it) }

      if (removeValuesNotPassed) {
        publishTheRemovalOfABunchOfStrategies(strategiesToDelete.values)
        database.deleteAll(strategiesToDelete.values)
      }
    }
  }

  private fun environmentToFeatureValues(acl: DbAcl, personIsAdmin: Boolean, filter: String?): EnvironmentFeatureValues? {
    val roles: List<RoleType> = if (personIsAdmin) {
        listOf(*RoleType.values())
    } else {
      convertUtils.splitEnvironmentRoles(acl.roles)
    }

    // we are never called where the environment is archived
    return if (roles.isEmpty()) {
      null
    } else EnvironmentFeatureValues()
      .environmentId(acl.environment.id)
      .environmentName(acl.environment.name)
      .priorEnvironmentId(if (acl.environment.priorEnvironment == null) null else acl.environment.priorEnvironment.id)
      .roles(roles)
      .features(featuresForEnvironment(acl.environment, filter))
  }

  private fun featuresForEnvironment(environment: DbEnvironment, filter: String?): List<FeatureValue> {
    var featureValueFinder = QDbFeatureValue()
      .environment.eq(environment)
      .feature.whenArchived.isNull

    if (filter != null)
      featureValueFinder = featureValueFinder.or().feature.description.icontains(filter).feature.key.icontains(filter).endOr()

    return featureValueFinder.findList().map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs) }
  }

  /*
   * This is the main call that the "features" page users to get data back
   */
  override fun findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
      appId: UUID,
      person: Person,
      filter: String?
  ): ApplicationFeatureValues? {
      Conversions.nonNullApplicationId(appId)
      Conversions.nonNullPerson(person)
    val dbPerson = convertUtils.byPerson(person)
    val app = convertUtils.byApplication(appId)
    if (app != null && dbPerson != null && app.whenArchived == null && dbPerson.whenArchived == null) {
      // they don't have permission, so deny them.
      if (!convertUtils.isPersonMemberOfPortfolioGroup(app.portfolio.id, dbPerson.id)) {
        return null
      }

      val empty = Opts.empty()
      val personAdmin = convertUtils.isPersonApplicationAdmin(dbPerson, app)
      val environmentOrderingMap: MutableMap<UUID, DbEnvironment> = HashMap()

      // the requirement is that we only send back environments they have at least READ access to, so
      // this finds the environments their group has access to
      val permEnvs =
        QDbAcl()
          .environment.whenArchived.isNull
          .environment.parentApplication.eq(app)
          .environment.parentApplication.whenArchived.isNull
          .environment.parentApplication.groupRolesAcl.fetch()
          .group.whenArchived.isNull
          .group.peopleInGroup.eq(
          dbPerson
        ).findList()
          .onEach { acl: DbAcl -> environmentOrderingMap[acl.environment.id] = acl.environment }
          .map { acl: DbAcl -> environmentToFeatureValues(acl, personAdmin, filter) }
          .filter { obj: EnvironmentFeatureValues? -> Objects.nonNull(obj) }
          .filter { efv: EnvironmentFeatureValues? ->
            efv!!.roles!!.isNotEmpty()
          }

      // the user has no permission to any environments and they aren't an admin, they shouldn't see anything
      if (permEnvs.isEmpty() && !personAdmin) {
        return null
      }

      val envs: MutableMap<UUID?, EnvironmentFeatureValues?> = HashMap()

      // merge any duplicates, this occurs because the database query can return duplicate lines
      permEnvs.forEach { e: EnvironmentFeatureValues? ->
        val original = envs[e!!.environmentId]
        if (original != null) { // merge them
          val originalFeatureValueIds =
            original.features!!.stream().map { obj: FeatureValue -> obj.id }.collect(Collectors.toSet())
          e.features!!.forEach { fv: FeatureValue ->
            if (!originalFeatureValueIds.contains(fv.id)) {
              original.features!!.add(fv)
            }
          }

          e.roles!!
            .forEach { rt: RoleType? ->
              if (!original.roles!!
                  .contains(rt)
              ) {
                original.roles!!.add(rt)
              }
            }
        } else {
          envs[e.environmentId] = e
        }
      }

      // now we have a flat-map of individual environments  the user has actual access to, but they may be an admin, so
      // if so, we need to fill those in
      if (permEnvs.isNotEmpty() || personAdmin) {
        // now go through all the environments for this app
        val environments =
          QDbEnvironment().whenArchived.isNull.order().name.desc().parentApplication.eq(app).findList()
        // envId, DbEnvi
        val roles = if (personAdmin) Arrays.asList(*RoleType.values()) else ArrayList()
        environments.forEach { e: DbEnvironment ->
          if (envs[e.id] == null) {
            environmentOrderingMap[e.id] = e
            val e1 = EnvironmentFeatureValues()
              .environmentName(e.name)
              .priorEnvironmentId(if (e.priorEnvironment == null) null else e.priorEnvironment.id)
              .environmentId(e.id)
              .roles(roles) // all access (as admin)
              .features(if (!personAdmin) ArrayList() else featuresForEnvironment(e, filter))
            envs[e1.environmentId] = e1
          }
        }
      }

      // we have to get all of them and sort them into order because this person may not have access
      // to all of them, so we will lose the sort order if we try and order them
      // so we get them all, sort them, and then pick them out of the map one by one
      val sortingEnvironments: List<DbEnvironment> =
          ArrayList(QDbEnvironment().select(QDbEnvironment.Alias.id).parentApplication.id.eq(appId).findList())
        EnvironmentUtils.sortEnvironments(sortingEnvironments)
      val finalValues: MutableList<EnvironmentFeatureValues> = ArrayList()
      sortingEnvironments.forEach { e: DbEnvironment ->
        val efv = envs[e.id]
        if (efv != null) {
          finalValues.add(efv)
        }
      }

      var appFeatureQuery = QDbApplicationFeature().whenArchived.isNull.parentApplication.eq(app)

      if (filter != null) {
        appFeatureQuery = appFeatureQuery.or().description.icontains(filter).key.icontains(filter).endOr()
      }

      return ApplicationFeatureValues()
        .applicationId(appId)
        .features(
          appFeatureQuery
            .findList()
            .map { f: DbApplicationFeature? -> convertUtils.toApplicationFeature(f, empty) }
            )
        .environments(finalValues)
    }
    return null
  }

  companion object {
    private val log = LoggerFactory.getLogger(FeatureSqlApi::class.java)
  }
}
