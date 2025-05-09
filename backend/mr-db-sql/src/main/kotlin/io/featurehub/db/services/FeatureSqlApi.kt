package io.featurehub.db.services

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.*
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbAcl
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.model.SharedRolloutStrategyVersion
import io.featurehub.db.model.query.*
import io.featurehub.db.publish.CacheSourceFeatureGroupApi
import io.featurehub.db.utils.EnvironmentUtils
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import jakarta.persistence.OptimisticLockException
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function
import kotlin.math.max

interface InternalFeatureApi {
  fun saveFeatureValue(featureValue: DbFeatureValue, forceUpdate: Boolean = false)
  fun forceVersionBump(featureIds: List<UUID>, envId: UUID)
  fun updatedApplicationStrategy(
    strategyForFeatureValue: DbStrategyForFeatureValue,
    originalStrategy: RolloutStrategy,
    personWhoUpdated: DbPerson
  )
  fun detachApplicationStrategy(
    strategyForFeatureValue: DbStrategyForFeatureValue,
    originalStrategy: RolloutStrategy,
    personWhoArchived: DbPerson
  )

  companion object  {
    fun toRolloutStrategy(appStrategy: DbApplicationRolloutStrategy): RolloutStrategy {
      return RolloutStrategy().id(appStrategy.shortUniqueCode)
        .percentage(appStrategy.strategy.percentage)
        .percentageAttributes(appStrategy.strategy.percentageAttributes)
        .attributes(appStrategy.strategy.attributes)
        .avatar(appStrategy.strategy.avatar)
        .colouring(appStrategy.strategy.colouring)
        .name(appStrategy.name).disabled(false)
    }

    fun toRolloutStrategy(sharedStrategy: DbStrategyForFeatureValue): RolloutStrategy {
      return toRolloutStrategy(sharedStrategy.rolloutStrategy).value(sharedStrategy.value)
    }
  }
}

class InternalFeatureSqlApi @Inject constructor(private val convertUtils: Conversions,
                                                private val cacheSource: CacheSource,
                                                private val featureMessagePublisher: FeatureMessagingPublisher,)  : InternalFeatureApi {
  private val log: Logger = LoggerFactory.getLogger(InternalFeatureSqlApi::class.java)

  override fun saveFeatureValue(featureValue: DbFeatureValue, forceUpdate: Boolean) {
    val originalVersion = featureValue.version

    if (forceUpdate) {
      featureValue.markAsDirty()
    }

    featureValue.save()

    if (originalVersion != featureValue.version) { // have we got auditing enabled and did the feature change
      // now saved a versioned copy
      DbFeatureValueVersion.fromDbFeatureValue(featureValue, featureValue.version).save()
    }
  }


  override fun forceVersionBump(featureIds: List<UUID>, envId: UUID) {
    // force a version change on all these features
    QDbFeatureValue()
      .feature.id.`in`(featureIds)
      .environment.id.eq(envId)
      .findList().forEach {
        saveFeatureValue(it, true)
      }
  }

  override fun updatedApplicationStrategy(
    strategyForFeatureValue: DbStrategyForFeatureValue,
    originalStrategy: RolloutStrategy,
    personWhoUpdated: DbPerson
  ) {
    // we need to bump the feature value version even though nothing ostensibly changed
    strategyForFeatureValue.featureValue.let { fv ->
      val priorStrategies = fv.sharedRolloutStrategies.map { InternalFeatureApi.toRolloutStrategy(it) }

      fv.whoUpdated = personWhoUpdated
      saveFeatureValue(fv, true)

      // this will cause an audit webhook automatically
      cacheSource.publishFeatureChange(fv)

      publishFeatureMessage(fv, "updated", priorStrategies, originalStrategy,
        InternalFeatureApi.toRolloutStrategy(strategyForFeatureValue))
    }
  }

  private fun publishFeatureMessage(fv: DbFeatureValue,
                                    action: String,
                                    priorStrategies: List<RolloutStrategy>,
                                    originalStrategy: RolloutStrategy,
                                    newStrategy: RolloutStrategy?) {
    val singleNotUpdated = SingleFeatureValueUpdate(hasChanged = false, updated = false, previous = false)

    try {
      // this is a more complex diff publish
      featureMessagePublisher.publish(
        FeatureMessagingParameter(
          fv, singleNotUpdated,
          SingleNullableFeatureValueUpdate(false, null, null), singleNotUpdated,
          MultiFeatureValueUpdate(),
          MultiFeatureValueUpdate(
            true,
            mutableListOf(RolloutStrategyUpdate(type = action, old = originalStrategy, new = newStrategy)),
            mutableListOf(),
            priorStrategies
          ),
          SingleNullableFeatureValueUpdate(true, fv.version - 1, fv.version)
        ), convertUtils.organizationId()
      )
    } catch (e: Exception) {
      log.error("failed to process publish request for feature {} (id {} in app {})",
        fv.feature.key, fv.feature.id, fv.feature.parentApplication.id, e)
    }
  }

  // this needs to remove the connection, create an audit trail, and publish a new record to Edge, and trigger webhooks
  override fun detachApplicationStrategy(
    strategyForFeatureValue: DbStrategyForFeatureValue,
    originalStrategy: RolloutStrategy,
    personWhoArchived: DbPerson
  ) {
    // this removes the strategy and forces the feature to update & create a new historical record
    QDbFeatureValue().id.eq(strategyForFeatureValue.featureValue.id).findOne()?.let { fv ->
      val priorStrategies = fv.sharedRolloutStrategies.map { InternalFeatureApi.toRolloutStrategy(it) }
      // hold onto it because the object will be deleted
      val originalValue = strategyForFeatureValue.value

      if (fv.sharedRolloutStrategies.removeIf { it.id == strategyForFeatureValue.id }) {
        fv.whoUpdated = personWhoArchived
        saveFeatureValue(fv, true)

        // this will cause an audit webhook automatically
        cacheSource.publishFeatureChange(fv)
        publishFeatureMessage(fv, "deleted",
          priorStrategies,
          originalStrategy.value(originalValue), null)
      }
    }
  }
}

class FeatureSqlApi @Inject constructor(
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val rolloutStrategyValidator: RolloutStrategyValidator,
  private val featureMessagePublisher: FeatureMessagingPublisher,
  private val featureGroupApi: CacheSourceFeatureGroupApi,
) : FeatureApi, FeatureUpdateBySDKApi {

  @ConfigKey("features.max-per-page")
  private var maxPagination: Int? = 10000
  private val internalFeatureApi: InternalFeatureApi

  init {
    DeclaredConfigResolver.resolve(this)
    internalFeatureApi = InternalFeatureSqlApi(convertUtils, cacheSource, featureMessagePublisher)
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
      onlyUpdateFeatureValueForEnvironment(
        featureValue, person, dbFeatureValue,
        changingDefaultValue = true,
        updatingLock = true
      )
    } else if (person.hasChangeValueRole() || person.hasLockRole() || person.hasUnlockRole()) {
      onlyCreateFeatureValueForEnvironment(eid, key, featureValue, person)
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

  @Throws(FeatureApi.NoAppropriateRole::class)
  private fun onlyCreateFeatureValueForEnvironment(
    eid: UUID, key: String, featureValue: FeatureValue,
    person: PersonFeaturePermission
  ): FeatureValue? {
    val environ = convertUtils.byEnvironment(eid) ?: return null
    val dbPerson = convertUtils.byPerson(person.person) ?: return null
    val appFeature = QDbApplicationFeature()
      .select(QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.key)
      .key.eq(key)
      .parentApplication.environments.eq(environ)
      .parentApplication.environments.whenArchived.isNull
      .findOne() ?: return null

    val featureValField = convertFeatureValueToDefaultValue(featureValue, appFeature.valueType)
    val typeBool = (appFeature.valueType == FeatureValueType.BOOLEAN)

    // they cannot set the value field if they don't have change value permissions or a boolean field to true for same
    if (!person.hasChangeValueRole() && ((featureValField != null && !typeBool) || (typeBool && featureValField != "false"))) {
      throw FeatureApi.NoAppropriateRole()
    }

    val locked = convertUtils.safeConvert(featureValue.locked)
    if ((typeBool && !locked && !person.hasUnlockRole()) || (!typeBool && !person.hasLockRole() && locked)) {
      throw FeatureApi.NoAppropriateRole()
    }

    val dbFeatureValue = DbFeatureValue(
      dbPerson,
      locked, appFeature, environ,
      featureValField
    )

    dbFeatureValue.let { dfv ->
      dfv.rolloutStrategies = convertStrategiesToDbFeatureValueStrategies(featureValue)
      featureValue.rolloutStrategyInstances?.let {
        dfv.sharedRolloutStrategies = convertApplicationStrategiesToSharedStrategies(dfv, it, appFeature.parentApplication.id)
      }
      dfv.retired = convertUtils.safeConvert(featureValue.retired)
    }

    save(dbFeatureValue)
    publish(dbFeatureValue)
    publishFirstRecord(dbFeatureValue, featureValue)

    return convertUtils.toFeatureValue(dbFeatureValue)
  }

  private fun convertApplicationStrategiesToSharedStrategies(
    dfv: DbFeatureValue,
    sharedStrategies: List<RolloutStrategyInstance>,
    appId: UUID
  ): MutableList<DbStrategyForFeatureValue> {
    val strategies = QDbApplicationRolloutStrategy().id.`in`(sharedStrategies.map { it.strategyId }.toSet()).application.id.eq(appId).findList().associateBy { it.id }

    return sharedStrategies
      .filter { strat ->  strategies[strat.strategyId] != null }
      .map { DbStrategyForFeatureValue.Builder()
        .featureValue(dfv)
        .value(it.value?.toString())
        .enabled(true)
        .rolloutStrategy(strategies[it.strategyId])
        .build() }
      .toMutableList()
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun save(featureValue: DbFeatureValue, forceUpdate: Boolean = false) {
    internalFeatureApi.saveFeatureValue(featureValue, forceUpdate)
  }

  private fun publish(featureValue: DbFeatureValue) {
    log.trace("publishing update for {}", featureValue)
    cacheSource.publishFeatureChange(featureValue)
  }

  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class)
  fun onlyUpdateFeatureValueForEnvironment(
    featureValue: FeatureValue,
    person: PersonFeaturePermission,
    existing: DbFeatureValue,
    changingDefaultValue: Boolean,
    updatingLock: Boolean,
  ): FeatureValue? {

    val dbPerson = convertUtils.byPerson(person.person) ?: return null

    val historical = QDbFeatureValueVersion().id.id.eq(featureValue.id).id.version.eq(featureValue.version).findOne()
    if (historical == null && existing.version != featureValue.version) {
      // there is no historical value to compare against and we aren't updating the existing version
      throw OptimisticLockingException()
    }

    if (historical == null) {
      log.trace("historical is null, updating old way")
      updateFeatureValue(featureValue, person, existing, existing.feature.valueType, dbPerson)

      save(existing, true)
      publish(existing)
      publishFirstRecord(existing, featureValue)
    } else {
      // saving is done inside here as it detects it
      updateSelectively(featureValue, person, existing, dbPerson, historical, changingDefaultValue, updatingLock)
    }

    return convertUtils.toFeatureValue(existing, Opts.opts(FillOpts.RolloutStrategies))
  }

  /**
   * This happens because we have never published this record before, so we need to gather it all up into a
   * brand new message
   */
  private fun publishFirstRecord(existing: DbFeatureValue, featureValue: FeatureValue) {
    val lockUpdate =
      SingleFeatureValueUpdate(hasChanged = featureValue.locked, updated = featureValue.locked, previous = false)
    val defaultValueUpdate = if (existing.feature.valueType == FeatureValueType.BOOLEAN)
      SingleNullableFeatureValueUpdate<String?>(
        hasChanged = existing.defaultValue !== "false",
        previous = "false",
        updated = existing.defaultValue
      )
    else SingleNullableFeatureValueUpdate<String?>(
      hasChanged = existing.defaultValue !== null,
      previous = null,
      updated = existing.defaultValue
    )
    val strategyUpdates = MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>()
    featureValue.rolloutStrategies?.let { rs ->
      strategyUpdates.hasChanged = rs.isNotEmpty()

      rs.forEach { strategy ->
        strategyUpdates.updated.add(RolloutStrategyUpdate(type = "added", new = strategy))
      }
    }
    val applicationStrategyUpdates = MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>()
    applicationStrategyUpdates.hasChanged = existing.sharedRolloutStrategies.isNotEmpty()
    existing.sharedRolloutStrategies.forEach { rsi ->
      applicationStrategyUpdates.updated.add(RolloutStrategyUpdate(type = "added", new = InternalFeatureApi.toRolloutStrategy(rsi)))
    }
    val retiredUpdate =
      SingleFeatureValueUpdate(hasChanged = featureValue.retired, updated = featureValue.retired, previous = false)
    publishChangesForMessaging(
      existing, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates,
      applicationStrategyUpdates,
      SingleNullableFeatureValueUpdate(true, featureValue.version, null)
    )

  }

  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class)
  internal fun updateSelectively(
    featureValue: FeatureValue,
    person: PersonFeaturePermission,
    existing: DbFeatureValue,
    dbPerson: DbPerson,
    historical: DbFeatureValueVersion,
    changingDefaultValue: Boolean,
    isFeatureValueChangeUpdatingLockValue: Boolean // this can be false when the update is coming from Edge
  ) {
    val feature = existing.feature

    val lockUpdate =
      if (isFeatureValueChangeUpdatingLockValue) updateSelectivelyLocked(featureValue, historical, existing, person)
      else SingleFeatureValueUpdate(hasChanged = false, updated = false, previous = false)

    val lockChanged = lockUpdate.hasChanged

    log.trace("before-update-default: {}, {}", existing.defaultValue, changingDefaultValue)
    // allow them to change the value and lock it at the same time
    val defaultValueUpdate = if (changingDefaultValue)
      updateSelectivelyDefaultValue(feature, featureValue, historical, existing, person, lockChanged)
    else SingleNullableFeatureValueUpdate()
    log.trace("after-update-default: {}, {}", existing.defaultValue, changingDefaultValue)

    val strategyUpdates = updateSelectivelyRolloutStrategies(person, featureValue, historical, existing, lockChanged)
    val applicationStrategyUpdates = updateSelectivelyApplicationRolloutStrategies(person, featureValue, historical,
      existing, lockChanged, existing.feature.parentApplication.id)

    val retiredUpdate = updateSelectivelyRetired(person, featureValue, historical, existing, lockChanged)

    // if the existing value is locked and this update didn't change it and we are actually changing something
    // we have an issue
    if (existing.isLocked && !lockChanged && (defaultValueUpdate.hasChanged || strategyUpdates.hasChanged || retiredUpdate.hasChanged)) {
      throw OptimisticLockingException()
    }

    if (lockChanged || defaultValueUpdate.hasChanged || strategyUpdates.hasChanged || retiredUpdate.hasChanged || applicationStrategyUpdates.hasChanged) {
      existing.whoUpdated = dbPerson
      save(existing, true)
      publish(existing)
      publishChangesForMessaging(
        existing, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates,
        applicationStrategyUpdates,
        SingleNullableFeatureValueUpdate(true, featureValue.version, historical.versionFrom)
      )
    } else {
      log.trace("update created no changes, not saving or publishing")
    }
  }

  private fun publishChangesForMessaging(
    featureValue: DbFeatureValue,
    lockUpdate: SingleFeatureValueUpdate<Boolean>,
    defaultValueUpdate: SingleNullableFeatureValueUpdate<String?>,
    retiredUpdate: SingleFeatureValueUpdate<Boolean>,
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
    applicationStrategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
    versionUpdate: SingleNullableFeatureValueUpdate<Long>
  ) {
    try {
      val featureMessagingParameter =
        FeatureMessagingParameter(
          featureValue,
          lockUpdate,
          defaultValueUpdate,
          retiredUpdate,
          strategyUpdates,
          applicationStrategyUpdates,
          versionUpdate
        )
      log.trace("publishing {}", featureMessagingParameter)
      featureMessagePublisher.publish(featureMessagingParameter, convertUtils.organizationId())
    } catch (e: Exception) {
      log.error("Failed to publish feature messaging update {}", featureValue, e)
    }
  }

  fun updateSelectivelyRetired(
    person: PersonFeaturePermission,
    featureValue: FeatureValue,
    historical: DbFeatureValueVersion,
    existing: DbFeatureValue,
    lockChanged: Boolean
  ): SingleFeatureValueUpdate<Boolean> {
    val retiredFeatureValueUpdate = SingleFeatureValueUpdate(updated = false, previous = false)
    val existingRetired = existing.retired // it can be null, which is also false

    // is it different from what it is now? if not, exit
    if (featureValue.retired == existingRetired) {
      return retiredFeatureValueUpdate
    }

    // did they actually change it? if not, exit
    if (featureValue.retired == historical.isRetired) {
      return retiredFeatureValueUpdate
    }

    if (historical.isRetired == existingRetired) { // but historical is the same as current
      if (existing.isLocked && !lockChanged) { // if its locked and we didn't change it to locked, we have to reject this change
        log.debug("feature value is locked, you cannot change it")
        throw FeatureApi.LockedException() // not really? its just locked so you can't change it
      }

      if (!person.hasChangeValueRole()) {
        log.debug("trying to change retired and no permission")
        throw FeatureApi.NoAppropriateRole()
      }

      existing.retired = convertUtils.safeConvert(featureValue.retired)

      val updated = featureValue.retired
      updateSingleFeatureValueUpdate(retiredFeatureValueUpdate, updated, historical.isRetired)

      return retiredFeatureValueUpdate
    }

    // otherwise they changed it from historical and existing has already changed
    throw OptimisticLockingException()
  }

  fun updateSelectivelyApplicationRolloutStrategies(
    person: PersonFeaturePermission,
    featureValue: FeatureValue,
    historicalFeatureValueVersion: DbFeatureValueVersion,
    existingDbFeatureValue: DbFeatureValue,
    lockChanged: Boolean,
    appId: UUID
  ): MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy> {
    val strategyUpdates = MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>()

    val incomingStrategyUpdates = featureValue.rolloutStrategyInstances ?: return strategyUpdates

    var changed = false
    val personCanChangeValues = person.hasChangeValueRole()

    val historical = historicalFeatureValueVersion.sharedRolloutStrategies.toList()
    val existing = existingDbFeatureValue.sharedRolloutStrategies.map { srs ->
      SharedRolloutStrategyVersion(srs.rolloutStrategy.id, srs.rolloutStrategy.version, true, srs.value)
    }.toMutableList()
    val originalSharedStategyList = existingDbFeatureValue.sharedRolloutStrategies

    // we need a map of the existing strategies in the historical version, and as we
    // loop over the passed strategies, we will detect if they are in  the map and if so, remove them.
    // this leaves us with a map of strategies in the historical entry that are deleted.
    val strategiesToDelete = historical.map { it.strategyId }.associateBy { it }.toMutableMap()

    // allows us to keep track of them
    val foundApplicationStrategies = mutableMapOf<UUID,DbApplicationRolloutStrategy>()

    for ((index, unresolvedIncomingStrategy) in incomingStrategyUpdates.withIndex()) {
      if (unresolvedIncomingStrategy == null) continue

      val appStrategy =
        QDbApplicationRolloutStrategy().application.id.eq(appId).id.eq(unresolvedIncomingStrategy.strategyId).findOne()
          ?: continue

      foundApplicationStrategies[appStrategy.id] = appStrategy

      val incomingSharedStrategy = toSharedRolloutStrategyVersion(appStrategy, unresolvedIncomingStrategy)

      // we are tracking strategies which don't turn up from the client update, if this one turned up, remove it from the delete list
      strategiesToDelete.remove(appStrategy.id)

      // does the strategy we have been passed exist in the historical strategy list?
      val historicalStrategy = historical.find { it.strategyId == appStrategy.id }

      // is this strategy  new? if so, it won't be in the list. the client adds ids, but only to associate
      // errors - we need to make sure they adhere to our rules now
      if (historicalStrategy == null) { // strategy.id == null || <- can never be because we removed all the null ones in rationaliseStrategyIdsAndAttributeIds
        if (!personCanChangeValues) {
          log.debug("trying to add strategy and no permission")
          throw FeatureApi.NoAppropriateRole()
        }

        changed = true

        val added = DbStrategyForFeatureValue.Builder().rolloutStrategy(appStrategy).enabled(true).value(incomingSharedStrategy.value?.toString()).build()
        if (index >= existing.size) {
          existing.add(incomingSharedStrategy)
          originalSharedStategyList.add(added)
        } else {
          // try and insert this new strategy where they have placed it
          existing.add(index, incomingSharedStrategy)
          originalSharedStategyList.add(index, added)
        }

        addToStrategyUpdates(
          type = "added",
          newStrategy = sharedStrategyToRolloutStrategyForReporting(incomingSharedStrategy, foundApplicationStrategies),
          strategyUpdates = strategyUpdates
        )
      } else {
        // its the same as the HISTORICAL one, then we have to assume that they haven't changed it, so we skip it
        if (Objects.deepEquals(incomingSharedStrategy, historicalStrategy)) {
          continue
        }

        // if this criteria matches, they have changed one that has been deleted, so thats a locking issue
        val currentStrategy = existing.find { it.strategyId == unresolvedIncomingStrategy.strategyId }
        if (currentStrategy == null) { // historically it existed, but its been deleted, so ignore it
          log.debug("feature value strategy was deleted")
          throw OptimisticLockingException() // it's been deleted
        }

        // now we have to detect if they have changed it
        // if the strategy is the same as the CURRENT one - it doesn't matter if they changed it, its the same as the current one
        if (Objects.deepEquals(incomingSharedStrategy, currentStrategy)) {
          continue
        }

        // when they changed it against their historical copy, is it the same as the current version?
        if (Objects.deepEquals(historicalStrategy, currentStrategy)) { // it hasn't changed from the historical one
          if (!personCanChangeValues) {
            log.debug("trying to add strategy and no permission")
            throw FeatureApi.NoAppropriateRole()
          }

          // ok - its all good to replace this one
          changed = true
          val pos = existing.indexOfFirst { it.strategyId == unresolvedIncomingStrategy.strategyId }
          existing[pos] = incomingSharedStrategy
          originalSharedStategyList[pos] = DbStrategyForFeatureValue.Builder().rolloutStrategy(appStrategy)
            .enabled(true).value(incomingSharedStrategy.value?.toString()).build()

          addToStrategyUpdates(
            type = "changed",
            newStrategy = sharedStrategyToRolloutStrategyForReporting(incomingSharedStrategy, foundApplicationStrategies),
            oldStrategy = sharedStrategyToRolloutStrategyForReporting(historicalStrategy, foundApplicationStrategies),
            strategyUpdates = strategyUpdates
          )
        } else {
          throw OptimisticLockingException() // it has changed since its history and user wants to change it again
        }
      }
    }

    // now we have to modify the _actual_ list that is attached to this feature value
    if (strategiesToDelete.isNotEmpty()) {
      if (!personCanChangeValues) {
        log.debug("trying to delete strategies and no permission")
        throw FeatureApi.NoAppropriateRole()
      }

      // remove all strategies where the strategy-id is in  the "to-delete" column
      originalSharedStategyList.removeIf { strategy ->
        // cache this as we need it for the historical mapping
        foundApplicationStrategies[strategy.rolloutStrategy.id] = strategy.rolloutStrategy

        val todel = strategiesToDelete.containsKey(strategy.rolloutStrategy.id)
        changed = true
        if (todel) {
          addToStrategyUpdates(type = "deleted",
            oldStrategy = InternalFeatureApi.toRolloutStrategy(strategy),
            strategyUpdates = strategyUpdates)
        }
        todel
      }
    }

    // none of the above checks if we simply _reordered_ the existing list of strategies, so now we need do check that
    // ok, now just honour the order of the incoming strategies and keep track if they actually changed

    // at this point, originalSharedStategyList contains all of the strategies but not in any particular order

    // this maps all the incoming updates to their now DbStrategyForFeatureValue variants
    val desiredList = incomingStrategyUpdates.mapNotNull { newStrategy ->
        originalSharedStategyList.find { it.rolloutStrategy.id == newStrategy.strategyId } }
        .toMutableList()
    val desiredOrderedIds = desiredList.map { it.rolloutStrategy.id }
    val historicalOrderedIds = historical.map { it.strategyId }

    if (desiredOrderedIds != historicalOrderedIds) {
      if (!personCanChangeValues) {
        log.debug("trying to reorder strategies and no change value permission")
        throw FeatureApi.NoAppropriateRole()
      }
      addToStrategyReorders(strategyUpdates, desiredList.map { InternalFeatureApi.toRolloutStrategy(it) }.toMutableList(),
        historical.map { sharedStrategyToRolloutStrategyForReporting(it, foundApplicationStrategies) })

      changed = true
    }

    if (changed && existingDbFeatureValue.isLocked && !lockChanged) {
      throw FeatureApi.LockedException()
    }

    // now the desiredList has been ordered according to the incoming strategies but picked from the
    // list of DbStrategyForFeatureValue's

    existingDbFeatureValue.sharedRolloutStrategies = desiredList

    strategyUpdates.hasChanged = changed

    return strategyUpdates
  }

  private fun sharedStrategyToRolloutStrategyForReporting(sharedRolloutStrategyVersion: SharedRolloutStrategyVersion,
                                                          appStrategies: Map<UUID,DbApplicationRolloutStrategy>): RolloutStrategy {
    val appStrategy = appStrategies[sharedRolloutStrategyVersion.strategyId] ?: QDbApplicationRolloutStrategy().id.eq(sharedRolloutStrategyVersion.strategyId).findOne()!!
    return RolloutStrategy().id(appStrategy.shortUniqueCode).name(appStrategy.name).value(sharedRolloutStrategyVersion.value).disabled(false)
  }
  private fun toSharedRolloutStrategyVersion(
    appStrategy: DbApplicationRolloutStrategy,
    incomingStrategyUpdate: RolloutStrategyInstance
  ): SharedRolloutStrategyVersion {
    return SharedRolloutStrategyVersion(appStrategy.id, appStrategy.version, true, incomingStrategyUpdate.value)
  }

  /**
   * here deleting and adding new ones isn't the issue. the issue is checking for updates to existing ones, and
   * we need to treat them the same as we do for a "default value".
   */
  fun updateSelectivelyRolloutStrategies(
    person: PersonFeaturePermission,
    featureValue: FeatureValue,
    historical: DbFeatureValueVersion,
    existing: DbFeatureValue,
    lockChanged: Boolean
  ): MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy> {
    val strategyUpdates = MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>()
    var changed = false
    val personCanChangeValues = person.hasChangeValueRole()

    val historicalStrategies = historical.rolloutStrategies.toList() // not mutable
    val existingStrategies = existing.rolloutStrategies.toMutableList()

    featureValue.rolloutStrategies?.let { strategies ->
      rationaliseStrategyIdsAndAttributeIds(strategies)

      // we need a map of the existing strategies in the historical version, and as we
      // loop over the passed strategies, we will detect if they are in  the map and if so, remove them.
      // this leaves us with a map of strategies in the historical entry that are deleted.
      val strategiesToDelete = historicalStrategies.map { it.id }.associateBy { it }.toMutableMap()

      for ((index, strategy) in strategies.withIndex()) {
        // we found this one, so remove it from the list
        strategiesToDelete.remove(strategy.id)

        // if the id = null, then there won't be one, otherwise try and match it
        val historicalStrategy = historicalStrategies.find { it.id == strategy.id }

        // is this strategy  new? if so, it won't be in the list. the client adds ids, but only to associate
        // errors - we need to make sure they adhere to our rules now
        if (historicalStrategy == null) { // strategy.id == null || <- can never be because we removed all the null ones in rationaliseStrategyIdsAndAttributeIds
          if (!personCanChangeValues) {
            log.debug("trying to add strategy and no permission")
            throw FeatureApi.NoAppropriateRole()
          }

          changed = true
          if (index >= existingStrategies.size) {
            existingStrategies.add(strategy)
          } else {
            // try and insert this new strategy where they have placed it
            existingStrategies.add(index, strategy)
          }
          addToStrategyUpdates(type = "added", newStrategy = strategy, strategyUpdates = strategyUpdates)
        } else {
          // its the same as the HISTORICAL one, then we have to assume that they haven't changed it, so we skip it
          if (Objects.deepEquals(strategy, historicalStrategy)) {
            continue
          }

          // if this criteria matches, they have changed one that has been deleted, so thats a locking issue
          val currentStrategy = existingStrategies.find { it.id == strategy.id }
          if (currentStrategy == null) { // historically it existed, but its been deleted, so ignore it
            log.debug("feature value strategy was deleted")
            throw OptimisticLockingException() // it's been deleted
          }

          // now we have to detect if they have changed it
          // if the strategy is the same as the CURRENT one - it doesn't matter if they changed it, its the same as the current one
          if (Objects.deepEquals(strategy, currentStrategy)) {
            continue
          }

          // when they changed it against their historical copy, is it the same as the current version?
          if (Objects.deepEquals(historicalStrategy, currentStrategy)) { // it hasn't changed from the historical one
            if (!personCanChangeValues) {
              log.debug("trying to add strategy and no permission")
              throw FeatureApi.NoAppropriateRole()
            }

            // ok - its all good to replace this one
            changed = true
            existingStrategies[existingStrategies.indexOfFirst { it.id == strategy.id }] = strategy
            if (strategy.id!!.length > strategyIdLength) {
              var newId = RandomStringUtils.randomAlphanumeric(strategyIdLength)
              while (existingStrategies.any { it.id == newId } || historicalStrategies.any { it.id == newId }) {
                newId = RandomStringUtils.randomAlphanumeric(strategyIdLength)
              }
              strategy.id = newId
            }
            addToStrategyUpdates(
              type = "changed",
              newStrategy = strategy,
              oldStrategy = historicalStrategy,
              strategyUpdates = strategyUpdates
            )
          } else {
            throw OptimisticLockingException() // it has changed since its history and user wants to change it again
          }
        }
      }

      if (strategiesToDelete.isNotEmpty()) {
        if (!personCanChangeValues) {
          log.debug("trying to delete strategies and no permission")
          throw FeatureApi.NoAppropriateRole()
        }

        // Collect strategies to delete and add to the strategy updates list
        existingStrategies.filter { existing -> strategiesToDelete.contains(existing.id) }
          .forEach { strategyToDelete ->
            changed = true
            existingStrategies.remove(strategyToDelete)
            addToStrategyUpdates(type = "deleted", oldStrategy = strategyToDelete, strategyUpdates = strategyUpdates)
          }
      }

      // ok, now just honour the order of the incoming strategies and keep track if they actually changed
      val newlyOrderedList =
        (featureValue.rolloutStrategies?.mapNotNull { newStrategy -> existingStrategies.find { it.id == newStrategy.id } }
          ?: listOf()).toMutableList()
      val newlyOrderedListIds = newlyOrderedList.map { it.id }
      newlyOrderedList.addAll(existingStrategies.filter { !newlyOrderedListIds.contains(it.id) })
      val reorderedList = newlyOrderedList.map { it.id }

      if (existingStrategies.map { it.id } != reorderedList) {
        if (!personCanChangeValues) {
          log.debug("trying to reorder strategies and no change value permission")
          throw FeatureApi.NoAppropriateRole()
        }
        addToStrategyReorders(strategyUpdates, newlyOrderedList, historicalStrategies)
        changed = true
      }

      existing.rolloutStrategies = newlyOrderedList
    }

    if (changed && existing.isLocked && !lockChanged) {
      throw FeatureApi.LockedException()
    }

    return strategyUpdates
  }

  private fun addToStrategyUpdates(
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
    type: String, newStrategy: RolloutStrategy? = null, oldStrategy: RolloutStrategy? = null
  ) {
    strategyUpdates.hasChanged = true
    val rollingStrategyUpdate = RolloutStrategyUpdate(type = type, new = newStrategy, old = oldStrategy)
    strategyUpdates.updated.add(rollingStrategyUpdate)
  }

  private fun addToStrategyReorders(
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
    reordered: MutableList<RolloutStrategy>, previous: List<RolloutStrategy>
  ) {
    strategyUpdates.hasChanged = true
    strategyUpdates.reordered = reordered
    strategyUpdates.previous = previous
  }

  private fun <T> updateSingleFeatureValueUpdate(
    featureValueUpdate: SingleFeatureValueUpdate<T>, updated: T, previous: T
  ): SingleFeatureValueUpdate<T> {
    featureValueUpdate.hasChanged = true
    featureValueUpdate.updated = updated
    featureValueUpdate.previous = previous
    return featureValueUpdate
  }

  /**
   * What we are trying to achieve is that if someone changes the value from what it was on the historical value
   * and the value hasn't changed between the historical value and the current value, then we can accept the change.
   */
  fun updateSelectivelyDefaultValue(
    feature: DbApplicationFeature,
    featureValue: FeatureValue,
    historical: DbFeatureValueVersion,
    existing: DbFeatureValue,
    person: PersonFeaturePermission,
    lockChanged: Boolean
  ): SingleNullableFeatureValueUpdate<String?> {
    val defaultValueUpdate = SingleNullableFeatureValueUpdate<String?>()
    val defaultValueChanged: String? =
      when (feature.valueType) {
        FeatureValueType.NUMBER -> {
          featureValue.valueNumber?.toString()
        }

        FeatureValueType.STRING -> {
          featureValue.valueString
        }

        FeatureValueType.JSON -> {
          featureValue.valueJson
        }

        FeatureValueType.BOOLEAN -> {
          featureValue.valueBoolean?.toString() ?: "false"
        }
      }

    // they aren't changing this value, so skip out
    if (defaultValueChanged == existing.defaultValue) {
      return defaultValueUpdate // nothing is changing, don't worry about it
    }

    // if it changed from the historical version
    if (defaultValueChanged == historical.defaultValue) {
      return defaultValueUpdate
    }
    // it didn't change between the  historical version and the current version?
    if (historical.defaultValue == existing.defaultValue) {
      if (existing.isLocked && !lockChanged) { // if its locked and we didn't change it to locked, we have to reject this change
        log.debug("feature value is locked, you cannot change it")
        throw FeatureApi.LockedException() // not really? its just locked so you can't change it
      }

      // as the value is different from the historical one, and the historical one is the _same_ as the current one
      // there must be an update, so we now check permissions
      if (!person.hasChangeValueRole()) {
        log.debug("Attempted to change value and has no permission")
        throw FeatureApi.NoAppropriateRole()
      }

      existing.defaultValue = defaultValueChanged

      defaultValueUpdate.hasChanged = true
      defaultValueUpdate.updated = defaultValueChanged
      defaultValueUpdate.previous = historical.defaultValue
      return defaultValueUpdate
    }

    // someone changed it in the meantime so they can't change it
    throw OptimisticLockingException()
  }

  // if they  are trying to change the locked status from the version they were on
  // AND it is also changing on the current version  then we  need to check if they can
  // change locked  permissions and reject them if they can't.
  // if they aren't changing the lock value from what it used to be, or they are changing it to
  // the value it currently is, it doesn't matter
  fun updateSelectivelyLocked(
    featureValue: FeatureValue,
    historical: DbFeatureValueVersion,
    existing: DbFeatureValue,
    person: PersonFeaturePermission
  ): SingleFeatureValueUpdate<Boolean> {
    val lockUpdate = SingleFeatureValueUpdate(updated = false, previous = false)
    val updatedValue = featureValue.locked
    if (updatedValue == existing.isLocked) {
      return lockUpdate
    }

    // if we changed the value from the historical version
    if (updatedValue != historical.isLocked) {
      // if the existing version is the same as the locked version, we can continue to check
      if (existing.isLocked == historical.isLocked) {
        if (updatedValue && !person.hasLockRole()) {
          log.debug("User is trying to lock a feature value and does not have lock permission")
          throw FeatureApi.NoAppropriateRole()
        }
        if (!updatedValue && !person.hasUnlockRole()) {
          log.debug("User is trying to unlock feature  value and does not have permission")
          throw FeatureApi.NoAppropriateRole()
        }

        existing.isLocked = updatedValue

        return updateSingleFeatureValueUpdate(lockUpdate, updatedValue, historical.isLocked)
      } else {
        // i can't actually see how this can happen
        throw OptimisticLockingException()
      }
    } // else didn't change it

    return lockUpdate
  }

  private fun convertFeatureValueToDefaultValue(featureValue: FeatureValue, valueType: FeatureValueType): String? {
    when (valueType) {
      FeatureValueType.NUMBER -> {
        return if (featureValue.valueNumber == null) null else featureValue.valueNumber.toString()
      }

      FeatureValueType.STRING -> {
        return featureValue.valueString
      }

      FeatureValueType.JSON -> {
        return featureValue.valueJson
      }

      FeatureValueType.BOOLEAN -> {
        return if (featureValue.valueBoolean == null) java.lang.Boolean.FALSE.toString()
        else featureValue.valueBoolean.toString()
      }
    }
  }

  private fun convertStrategiesToDbFeatureValueStrategies(featureValue: FeatureValue): List<RolloutStrategy> {
    return featureValue.rolloutStrategies?.let { strategies ->
      rationaliseStrategyIdsAndAttributeIds(strategies)
      strategies
    } ?: listOf()
  }


  /**
   * this is the original logic - do not touch, it is also used for the creation of the feature value the first time
   */
  @Throws(FeatureApi.NoAppropriateRole::class)
  private fun updateFeatureValue(
    featureValue: FeatureValue,
    person: PersonFeaturePermission,
    dbFeatureValue: DbFeatureValue,
    valueType: FeatureValueType,
    dbPerson: DbPerson
  ) {
    if (person.hasChangeValueRole() && (!dbFeatureValue.isLocked || java.lang.Boolean.FALSE == featureValue.locked && person.hasUnlockRole())) {
      dbFeatureValue.defaultValue = convertFeatureValueToDefaultValue(featureValue, valueType)
      dbFeatureValue.rolloutStrategies = convertStrategiesToDbFeatureValueStrategies(featureValue)
      // this should only hit on the 1st try around.
      dbFeatureValue.sharedRolloutStrategies =
        updateSharedRolloutStrategies(featureValue, dbFeatureValue, dbFeatureValue.feature.parentApplication.id)
    }

    // change locked before changing value, as may not be able to change value if locked
    val newValue = convertUtils.safeConvert(featureValue.locked)
    if (newValue != dbFeatureValue.isLocked) {
      if (!newValue && person.hasUnlockRole()) {
        dbFeatureValue.isLocked = false
      } else if (newValue && person.hasLockRole()) {
        dbFeatureValue.isLocked = true
      } else {
        throw FeatureApi.NoAppropriateRole()
      }
    }

    dbFeatureValue.whoUpdated = dbPerson

    if (person.hasChangeValueRole()) {
      dbFeatureValue.retired = convertUtils.safeConvert(featureValue.retired)
    }
  }

  private fun updateSharedRolloutStrategies(
    featureValue: FeatureValue,
    dbFeatureValue: DbFeatureValue,
    appId: UUID
  ): MutableList<DbStrategyForFeatureValue> {
    val updatedStrategies = featureValue.rolloutStrategyInstances?.toMutableList() ?: mutableListOf()
    val existing = dbFeatureValue.sharedRolloutStrategies.toMutableList()
    val newList = mutableListOf<DbStrategyForFeatureValue>()

    // we have to preserve this new order, so we do it in the order they give us
    updatedStrategies.forEach { s ->
      val match = existing.find { strategy -> strategy.rolloutStrategy.id == s.strategyId }
      if (match != null) { // already in the list
        match.value = s.value?.toString()
        newList.add(match)
      } else { // new shared strategy reference
        QDbApplicationRolloutStrategy().application.id.eq(appId).id.eq(s.strategyId).findOne()
          ?.let { foundAppStrategy ->
            newList.add(
              DbStrategyForFeatureValue.Builder().featureValue(dbFeatureValue)
                .rolloutStrategy(foundAppStrategy).value(s.value?.toString()).enabled(true).build()
            )
          }
      }
    }

    return newList
  }

  private fun rationaliseStrategyIdsAndAttributeIds(strategies: List<RolloutStrategy>): Boolean {
    var changes = false

    strategies.forEach { strategy ->
      if (strategy.id == null || strategy.id!!.length > strategyIdLength) {
        var id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
        // make sure it is unique
        while (strategies.any { id == strategy.id }) {
          id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
        }
        changes = true
        strategy.id = id
      }

      strategy.attributes?.forEach { attribute ->
        if (attribute.id == null || attribute.id!!.length > strategyIdLength) {
          var id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
          // make sure it is unique
          while (strategy.attributes!!.any { id == attribute.id }) {
            id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
          }

          changes = true
          attribute.id = id
        }
      }
    }

    return changes
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
        onlyUpdateFeatureValueForEnvironment(
          fv, requireRoleCheck, strategy,
          changingDefaultValue = true,
          updatingLock = true
        )
      }
    }

    // now for the creates
    for (key in newValues.keys) {
      newValues[key]?.let { fv ->
        onlyCreateFeatureValueForEnvironment(eid, key, fv, requireRoleCheck)
      }
    }

    return QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
      .map { fs: DbFeatureValue -> convertUtils.toFeatureValue(fs)!! }
  }

  @Throws(RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateFeatureFromTestSdk(
    sdkUrl: String, envId: UUID, featureKey: String, updatingValue: Boolean,
    updatingLock: Boolean,
    buildFeatureValue: Function<FeatureValueType, FeatureValue>
  ) {
    val account = QDbServiceAccount()
      .select(QDbServiceAccount.Alias.sdkPerson.id)
      .whenArchived.isNull
      .or().apiKeyClientEval.eq(sdkUrl).apiKeyServerEval.eq(sdkUrl).endOr()
      .findOne() ?: return

    val feature = QDbApplicationFeature()
      .select(QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.valueType)
      .parentApplication.environments.id.eq(envId).key.eq(featureKey)
      .findOne()
      ?: return

    // we need to know the current version if it exists at all

    // not checking permissions, edge checks those
    val newValue = buildFeatureValue.apply(feature.valueType)

    var fv = QDbFeatureValue().environment.id.eq(envId).feature.eq(feature).findOne()
    fv?.let {
      newValue.version(it.version)
      newValue.id(it.id)
    }

    val perms = PersonFeaturePermission(
      Person().id(PersonId().id(account.sdkPerson.id)),
      setOf(RoleType.READ, RoleType.CHANGE_VALUE, RoleType.LOCK, RoleType.UNLOCK)
    )

    var count = 0
    var saved = false
    while (count++ < 10 && !saved) {
      try {
        if (fv == null) {
          log.trace("test-sdk: creating value")
          onlyCreateFeatureValueForEnvironment(envId, featureKey, newValue, perms)
        } else {
          onlyUpdateFeatureValueForEnvironment(newValue, perms, fv, updatingValue, updatingLock)
        }
        saved = true
      } catch (ignored: OptimisticLockException) {
        log.trace("WARN: missed test-sdk due to optimistic lock {}", count)
        if (fv == null) {
          fv = QDbFeatureValue().environment.id.eq(envId).feature.eq(feature).findOne()
        } else {
          fv.refresh()
        }

        with(fv!!) {
          newValue.version(version)
          newValue.id(id)
        }
      }
    }
    if (!saved) {
      log.error("Unable to save feature update from TestSDK due to continual optimistic lock issues")
    }
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

      if (rationaliseStrategyIdsAndAttributeIds(fv.rolloutStrategies)) {
        save(fv)
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
