package io.featurehub.db.services

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyValidator
import io.featurehub.db.model.DbAcl
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.query.QDbAcl
import io.featurehub.db.model.query.QDbApplication
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbEnvironment
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.db.utils.EnvironmentUtils
import io.featurehub.mr.model.ApplicationFeatureValues
import io.featurehub.mr.model.ApplicationRoleType
import io.featurehub.mr.model.EnvironmentFeatureValues
import io.featurehub.mr.model.EnvironmentFeaturesResult
import io.featurehub.mr.model.Feature
import io.featurehub.mr.model.FeatureEnvironment
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.RoleType
import io.featurehub.mr.model.SortOrder
import io.featurehub.mr.model.ThinGroupRolloutStrategy
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

class FeatureSqlApi @Inject constructor(
  private val convertUtils: Conversions,
  private val rolloutStrategyValidator: RolloutStrategyValidator,
  private val featureGroupApi: CacheSourceFeatureGroupApi,
  private val updateFeatureApi: UpdateFeatureApi
) : FeatureApi {

  @ConfigKey("features.max-per-page")
  private var maxPagination: Int? = 10000

  init {
    DeclaredConfigResolver.resolve(this)
  }

  @Throws(
    OptimisticLockingException::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    FeatureApi.NoAppropriateRole::class,
    FeatureApi.LockedException::class
  )

  override fun createFeatureValueForEnvironment(
    eid: UUID,
    key: String,
    featureValue: FeatureValue,
    person: PersonFeaturePermission
  ): FeatureValue? {
    if (!person.hasWriteRole()) {
      val env = QDbEnvironment().select(QDbEnvironment.Alias.id).id.eq(eid).whenArchived.isNull.findOne()
      log.warn("User has no roles for environment {} key {}", eid, key)
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

    val dbFeatureValue = QDbFeatureValue().environment.id.eq(eid).feature.key.eq(key).findOne()

    return if (dbFeatureValue != null) {
      rolloutStrategyValidator.validateStrategies(
        dbFeatureValue.feature.valueType,
        featureValue.rolloutStrategies ?: listOf(),
        featureValue.rolloutStrategyInstances ?: listOf()
      ).hasFailedValidation()

      // this is an update not a create, environment + app-feature key exists
      updateFeatureApi.onlyUpdateFeatureValueForEnvironment(
        featureValue, person, dbFeatureValue,
        changingDefaultValue = true,
        updatingLock = true
      )
    } else if (person.hasChangeValueRole() || person.hasLockRole() || person.hasUnlockRole()) {
      updateFeatureApi.onlyCreateFeatureValueForEnvironment(eid, key, featureValue, person)
    } else {
      log.info(
        "roles for person are {} and are not enough for environment {} and key {}",
        person.toString(),
        eid,
        key
      )
      throw FeatureApi.NoAppropriateRole()
    }
  }

  @Throws(
    OptimisticLockingException::class,
    FeatureApi.NoAppropriateRole::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    FeatureApi.LockedException::class
  )
  override fun updateFeatureValueForEnvironment(
    eid: UUID,
    key: String,
    featureValue: FeatureValue,
    person: PersonFeaturePermission
  ): FeatureValue? {
    return createFeatureValueForEnvironment(eid, key, featureValue, person)
  }

  override fun getFeatureValueForEnvironment(eid: UUID, key: String): FeatureValue? {
    Conversions.nonNullEnvironmentId(eid)
    val featureValue =
      QDbFeatureValue().environment.id.eq(eid).feature.key.eq(key).sharedRolloutStrategies.fetch().findOne()
    return if (featureValue == null) null else convertUtils.toFeatureValue(featureValue, Opts.opts(FillOpts.RolloutStrategies))
  }

  override fun getAllFeatureValuesForEnvironment(eid: UUID, includeFeatures: Boolean): EnvironmentFeaturesResult {
    val environment =
      QDbEnvironment().select(QDbEnvironment.Alias.id).parentApplication.fetch(QDbApplication.Alias.id).id.eq(eid)
        .findOne() ?: return EnvironmentFeaturesResult()

    val env = EnvironmentFeaturesResult()
      .featureValues(
        QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
          .map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs) }
      )
      .environments(listOf(convertUtils.toEnvironment(QDbEnvironment().id.eq(eid).findOne(), Opts.empty())))

    if (includeFeatures) {
      env.features(
        QDbApplicationFeature().parentApplication.eq(environment.parentApplication).whenArchived.isNull.findList().map {
          convertUtils.toApplicationFeature(it, Opts.empty())
        })
    }

    return env
  }

  // we are going to have to put a transaction at this level as we want the whole thing to roll back if there is an issue
  @Throws(
    OptimisticLockingException::class,
    FeatureApi.NoAppropriateRole::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    FeatureApi.LockedException::class
  )
  override fun updateAllFeatureValuesForEnvironment(
    eid: UUID,
    featureValues: List<FeatureValue>,
    requireRoleCheck: PersonFeaturePermission
  ): List<FeatureValue> {
    require(
      featureValues.size == featureValues.map { obj: FeatureValue -> obj.key }.toSet().size
    ) { "Invalid update dataset" }

    val existingValues = QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
    val validFeatureKeys =
      QDbApplicationFeature().parentApplication.environments.id.eq(eid).findList().associateBy { it.key }
    val newValues = featureValues.associateBy { it.key }.toMutableMap()

    // ensure the strategies are valid from a conceptual perspective
    val failure = RolloutStrategyValidator.ValidationFailure()
    for (fv in featureValues) {
      val feat = validFeatureKeys[fv.key] ?: throw FeatureApi.NoSuchFeature()

      rolloutStrategyValidator.validateStrategies(
        feat.valueType,
        fv.rolloutStrategies ?: listOf(), fv.rolloutStrategyInstances ?: listOf(), failure
      )
    }
    failure.hasFailedValidation()

    for (strategy in existingValues) {
      val fv = newValues.remove(strategy.feature.key)
      if (fv != null) {
        // its existing, so update it
        updateFeatureApi.onlyUpdateFeatureValueForEnvironment(
          fv, requireRoleCheck, strategy,
          changingDefaultValue = true,
          updatingLock = true
        )
      }
    }

    // now for the creates
    for (key in newValues.keys) {
      newValues[key]?.let { fv ->
        updateFeatureApi.onlyCreateFeatureValueForEnvironment(eid, key, fv, requireRoleCheck)
      }
    }

    return QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
      .map { fs: DbFeatureValue -> convertUtils.toFeatureValue(fs)!! }
  }

  internal class EnvironmentsAndFeatureValues(
    // this is envId -> DbFeatureValue
    var envIdToDbFeatureValue: MutableMap<UUID, DbFeatureValue>,
    val feature: DbApplicationFeature,
    var envIdToRolePermissions: Map<UUID, Set<RoleType>>,
    var environments: Map<UUID, DbEnvironment>,
    var appRolesForThisPerson: Set<ApplicationRoleType>
  )

  private fun featureValuesUserCanAccess(appId: UUID, key: String, person: Person): EnvironmentsAndFeatureValues {
    val feature = QDbApplicationFeature()
      .key.eq(key)
      .parentApplication.id.eq(appId)
      .parentApplication.whenArchived.isNull.findOne()

    if (feature == null) {
      log.trace("User {} attempting to update feature that does not exist", person)
      throw FeatureApi.NoSuchFeature()
    }

    val featureValuesResult = mutableMapOf<UUID, DbFeatureValue>()
    val roles = mutableMapOf<UUID, MutableSet<RoleType>>()
    val environments = mutableMapOf<UUID, DbEnvironment>()
    val personAdmin = convertUtils.isPersonApplicationAdmin(person.id!!.id, appId)
    // this can be empty as the feature may not have a value in this environment if it is a non-bool
    val featureValueList = QDbFeatureValue()
      .feature.key.eq(key)
      .feature.parentApplication.id.eq(appId)
      .findList()
    val envIdToDbFeatureValues = featureValueList
      .associateBy { it.environment.id }.toMutableMap()

    val adminRoles = RoleType.entries
    if (!personAdmin) { // is they aren't a portfolio admin, figure out what their permissions are to each environment
      QDbAcl().environment.parentApplication.id.eq(appId).group.groupMembers.person.id.eq(person.id!!.id).findList()
        .forEach { fe: DbAcl ->
          log.debug(
            "Found environment `{}`, app `{}`, group `{}`, roles `{}`",
            if (fe.environment == null) "<none>" else fe.environment.name,
            if (fe.application == null) "<none>" else fe.application.name,
            fe.group.name, fe.roles
          )
          val roleTypes = convertUtils.splitEnvironmentRoles(fe.roles)
          if (roleTypes.isNotEmpty()) {
            val envId = fe.environment.id
            if (roles[envId] == null) {
              roles[envId] = mutableSetOf()
            }
            roles[envId]?.addAll(roleTypes)
            environments[envId] = fe.environment
            envIdToDbFeatureValues.remove(envId)?.let { fv ->
              featureValuesResult[envId] = fv
            }
          }
        }
    }

    // they have at least one environment or they are an admin
    if (environments.isNotEmpty() || personAdmin) {
      val emptyRoles = emptyList<RoleType>()
      QDbEnvironment().parentApplication.id.eq(appId).findList().forEach { env: DbEnvironment ->
        if (environments[env.id] == null) {
          environments[env.id] = env
          roles[env.id] = mutableSetOf()
          roles[env.id]?.addAll(if (personAdmin) adminRoles else emptyRoles)
          if (personAdmin) {
            envIdToDbFeatureValues.remove(env.id)?.let { fv ->
              featureValuesResult[env.id] = fv
            }
          }
        }
      }
    }
    val appRoles = QDbAcl().application.isNotNull
      .select(QDbAcl.Alias.roles)
      .roles.isNotNull
      .group.whenArchived.isNull
      .group.owningPortfolio.applications.id.eq(appId)
      .group.groupMembers.person.id.eq(person.id!!.id).findList()
      .map { appAcl: DbAcl -> convertUtils.splitApplicationRoles(appAcl.roles) }
      .flatten().toSet()

    return EnvironmentsAndFeatureValues(featureValuesResult, feature, roles, environments, appRoles)
  }

  override fun getFeatureValuesForApplicationForKeyForPerson(
    appId: UUID,
    key: String,
    person: Person
  ): List<FeatureEnvironment> {
    val result = featureValuesUserCanAccess(appId, key, person)

    return result.environments.keys.map { e ->
      convertUtils.toFeatureEnvironment(
        result.envIdToDbFeatureValue[e], result.envIdToRolePermissions[e]?.toList() ?: listOf(),
        result.environments[e]!!, Opts.opts(FillOpts.ServiceAccounts)
      )
    }
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  @Throws(
    OptimisticLockingException::class,
    FeatureApi.NoAppropriateRole::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    FeatureApi.LockedException::class
  )
  override fun updateAllFeatureValuesByApplicationForKey(
    id: UUID,
    key: String,
    featureValue: List<FeatureValue>,
    from: Person,
  ) {
    val result = featureValuesUserCanAccess(id, key, from)
    val failure = RolloutStrategyValidator.ValidationFailure()

    // environment id -> role in that environment
    val environmentToRoleMap = result.envIdToRolePermissions
    for (fv in featureValue) {
      // feature exists but it is the wrong environment?
      val envId = fv.environmentId
      if (envId == null) {
        log.warn("Trying to update for environment `{}` and environment id is invalid.", fv.environmentId)
        throw FeatureApi.NoAppropriateRole()
      }

      val roles = environmentToRoleMap[envId]

      if (roles == null || (roles.size == 1 && roles.contains(RoleType.READ))) {
        log.warn(
          "Trying to update for environment `{}` and environment id has no roles (no permissions).",
          envId
        )
        throw FeatureApi.NoAppropriateRole()
      }

      rolloutStrategyValidator.validateStrategies(
        result.feature.valueType,
        fv.rolloutStrategies ?: listOf(),
        fv.rolloutStrategyInstances ?: listOf(), failure
      )
    }

    // ok, they are allowed to update the stuff they are sending, but is it bad? this will throw an InvalidStrategyCombination
    failure.hasFailedValidation()

    // environment -> feature value
    val featureValuesToNull = result.envIdToDbFeatureValue
    for (fv in featureValue) {
      createFeatureValueForEnvironment(
        fv.environmentId!!, key, fv,
        PersonFeaturePermission.Builder()
          .person(from)
          .appRoles(result.appRolesForThisPerson)
          .roles(HashSet(environmentToRoleMap[fv.environmentId!!]!!)).build()
      )

      featureValuesToNull.remove(fv.environmentId) // we processed this environment ok, didn't throw a wobbly
    }

  }

  private fun environmentToFeatureValues(
    acl: DbAcl,
    personIsAdmin: Boolean,
    featureKeys: List<String>
  ): EnvironmentFeatureValues? {
    val roles: List<RoleType> = if (personIsAdmin) {
      RoleType.entries
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
      .features(featuresForEnvironment(acl.environment, featureKeys))
  }

  private fun featuresForEnvironment(environment: DbEnvironment, featureKeys: List<String>): List<FeatureValue> {
    val featureValueFinder = QDbFeatureValue()
      .environment.eq(environment)
      .environment.whenArchived.isNull
      .sharedRolloutStrategies.fetch()
      .feature.whenArchived.isNull
      .feature.key.`in`(featureKeys)

    return featureValueFinder.findList().map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs, Opts.opts(FillOpts.RolloutStrategies))!! }
  }

  /*
   * This is the main call that the "features" page users to get data back
   */
  override fun findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
    appId: UUID,
    current: Person,
    filter: String?,
    maxFeatures: Int?,
    startingPage: Int?,
    featureValueTypes: List<FeatureValueType>?,
    sortOrder: SortOrder?,
    environmentIds: List<UUID>?
  ): ApplicationFeatureValues? {
    val dbPerson = convertUtils.byPerson(current)
    val app = convertUtils.byApplication(appId)
    if (app != null && dbPerson != null && app.whenArchived == null && dbPerson.whenArchived == null) {
      // they don't have permission, so deny them.
      if (!convertUtils.isPersonMemberOfPortfolioGroup(app.portfolio.id, dbPerson.id)) {
        return null
      }

      val max = if (maxFeatures != null) max(maxFeatures, 1).coerceAtMost(maxPagination!!) else maxPagination!!
      val page = if (startingPage != null && startingPage >= 0) startingPage else 0
      val sort = sortOrder ?: SortOrder.ASC
      val empty = Opts.empty()

      var appFeatureQuery = QDbApplicationFeature()
        .whenArchived.isNull
        .parentApplication.eq(app)

      appFeatureQuery = if (sort == SortOrder.ASC) {
        appFeatureQuery.orderBy().key.asc()
      } else {
        appFeatureQuery.orderBy().key.desc()
      }

      if (filter != null) {
        appFeatureQuery = appFeatureQuery.or().name.icontains(filter).key.icontains(filter).endOr()
      }

      if (featureValueTypes?.isNotEmpty() == true) {
        appFeatureQuery = appFeatureQuery.valueType.`in`(featureValueTypes)
      }

      // this does not work with findFutureCount for some reason
      val totalFeatureCount = appFeatureQuery.findCount()

      val limitingAppFeatureQuery = appFeatureQuery.setFirstRow(page * max)
        .setMaxRows(max)

      val features = limitingAppFeatureQuery
        .findList()
        .map { f: DbApplicationFeature? -> convertUtils.toApplicationFeature(f, empty)!! }

      val featureKeys = features.map { f -> f.key }

      // because we need to know what features there are on what pages, we grab the application features we are going
      // to use *first*
      val personAdmin = convertUtils.isPersonApplicationAdmin(dbPerson, app)
      val environmentOrderingMap: MutableMap<UUID, DbEnvironment> = HashMap()


      // the requirement is that we only send back environments they have at least READ access to, so
      // this finds the environments their group has access to
      var rawPermsQl = QDbAcl()
        .environment.whenArchived.isNull
        .environment.parentApplication.eq(app)
        .environment.parentApplication.whenArchived.isNull
        .environment.parentApplication.groupRolesAcl.fetch()
        .group.whenArchived.isNull
        .group.groupMembers.person.eq(
          dbPerson
        )
      // it doesn't matter if they don't have access to the envs they are selecting, they still have
      // to have ACLs in them, but it allows us to limit them.
      environmentIds?.let { requestedEnvIdList ->
        if (requestedEnvIdList.isNotEmpty()) {
          rawPermsQl = rawPermsQl.environment.id.`in`(requestedEnvIdList)
        }
      }
      val permEnvs =
        rawPermsQl.findList()
          .onEach { acl: DbAcl -> environmentOrderingMap[acl.environment.id] = acl.environment }
          .mapNotNull { acl: DbAcl -> environmentToFeatureValues(acl, personAdmin, featureKeys) }
          .filter { efv: EnvironmentFeatureValues? ->
            efv!!.roles.isNotEmpty()
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
            original.features.map { obj: FeatureValue -> obj.id }.toSet()
          e.features.forEach { fv: FeatureValue ->
            if (!originalFeatureValueIds.contains(fv.id)) {
              original.features.add(fv)
            }
          }

          e.roles
            .forEach { rt: RoleType? ->
              if (!original.roles
                  .contains(rt)
              ) {
                original.roles.add(rt)
              }
            }
        } else {
          envs[e.environmentId] = e
        }
      }

      // now we have a flat-map of individual environments  the user has actual access to, but they may be an admin, so
      // if so, we need to fill those in. This gives us all the environments and sets the roles to empty
      // if the user has no access to the environment
      if (permEnvs.isNotEmpty() || personAdmin) {
        // now go through all the environments for this app
        var environmentsQl =
          QDbEnvironment().whenArchived.isNull.orderBy().name.desc().parentApplication.eq(app)
        environmentIds?.let { requestedEnvIdList ->
          if (requestedEnvIdList.isNotEmpty()) {
            environmentsQl = environmentsQl.id.`in`(requestedEnvIdList)
          }
        }
        val environments = environmentsQl.findList()
        // envId, DbEnvi
        val roles = if (personAdmin) RoleType.entries else listOf()
        environments.forEach { e: DbEnvironment ->
          if (envs[e.id] == null) {
            environmentOrderingMap[e.id] = e
            val e1 = EnvironmentFeatureValues()
              .environmentName(e.name)
              .priorEnvironmentId(if (e.priorEnvironment == null) null else e.priorEnvironment.id)
              .environmentId(e.id)
              .roles(roles) // all access (as admin)
              .features(if (!personAdmin) ArrayList() else featuresForEnvironment(e, featureKeys))
            envs[e1.environmentId] = e1
          }
        }
      }

      // we have to get all of them and sort them into order because this person may not have access
      // to all of them, so we will lose the sort order if we try and order them
      // so we get them all, sort them, and then pick them out of the map one by one
      val sortingEnvironments: List<DbEnvironment> =
        ArrayList(QDbEnvironment().select(QDbEnvironment.Alias.id).parentApplication.id.eq(appId).whenArchived.isNull.findList())
      EnvironmentUtils.sortEnvironments(sortingEnvironments)

      val finalValues = mutableListOf<EnvironmentFeatureValues>()
      sortingEnvironments.forEach { e: DbEnvironment ->
        envs[e.id]?.let { finalValues.add(it) }
      }

      fillInFeatureGroupData(features, finalValues)

      // this actually returns ALL environments regardless of whether a user has access, it simply returns
      // roles of [] if they don't have access
      return ApplicationFeatureValues()
        .applicationId(appId)
        .features(features)
        .environments(finalValues)
        .maxFeatures(totalFeatureCount)
    }
    return null
  }

  private fun fillInFeatureGroupData(features: List<Feature>, values: List<EnvironmentFeatureValues>) {
    val featureKeymap = features.associateBy { it.id }
    val envMap = values.associateBy { it.environmentId }

    // now will get back a bunch of name/value/envId/featureId pairs,
    for (fg in featureGroupApi.collectStrategiesFromEnvironmentsWithFeatures(
      values.map { it.environmentId }.distinct(),
      features.map { it.id!! }.distinct()
    )) {
      val key = featureKeymap[fg.featureId]?.key ?: continue
      val env = envMap[fg.envId] ?: continue

      env.features.find { it.key == key }
        ?.addFeatureGroupStrategiesItem(ThinGroupRolloutStrategy().name(fg.name).value(fg.value).featureGroupId(fg.featureGroupId))
    }
  }

  // it is already in a transaction for the job table, so it needs a new one
  @Transactional(type = TxType.REQUIRES_NEW)
  override fun release1_5_11_strategy_update() {
    log.info(
      "1.5.11 requires updating of the internal structure of strategies and the creation of a historical record, there are {} to process",
      QDbFeatureValue().findCount()
    )

    var count = 0

    QDbFeatureValue().findList().forEach { fv ->
      count++

      if (updateFeatureApi.rationaliseStrategyIdsAndAttributeIds(fv.rolloutStrategies)) {
        updateFeatureApi.save(fv)
      }

      if (count % 50 == 0) {
        log.info("upgraded {} feature values", count)
      }
    }

    log.info("feature value upgrade complete")
  }


  override fun duplicateRolloutStrategyInstances(featureValue: FeatureValue): Boolean {
    featureValue.rolloutStrategyInstances?.let { rsInstances ->
      val dupes = mutableMapOf<UUID,UUID>()

      for(rsi in rsInstances) {
        if (dupes.containsKey(rsi.strategyId)) {
          return true
        }

        dupes[rsi.strategyId] = rsi.strategyId
      }
    }

    return false
  }

  companion object {
    private val log = LoggerFactory.getLogger(FeatureSqlApi::class.java)

    const val strategyIdLength = 4
  }
}
