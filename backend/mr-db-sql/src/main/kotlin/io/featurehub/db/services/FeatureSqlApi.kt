package io.featurehub.db.services

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.ebean.Database
import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.*
import io.featurehub.db.listener.FeatureUpdateBySDKApi
import io.featurehub.db.model.DbAcl
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.query.*
import io.featurehub.db.utils.EnvironmentUtils
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.converter.FeatureMessagingParameter
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.*
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Function
import kotlin.math.max

interface InternalFeatureSqlApi {
  fun saveFeatureValue(featureValue: DbFeatureValue)
}

class FeatureSqlApi @Inject constructor(
  private val database: Database, private val convertUtils: Conversions, private val cacheSource: CacheSource,
  private val rolloutStrategyValidator: RolloutStrategyValidator,
  private val featureMessagingCloudEventPublisher: FeatureMessagingCloudEventPublisher,
) : FeatureApi, FeatureUpdateBySDKApi, InternalFeatureSqlApi {

  @ConfigKey("auditing.enable")
  var auditingEnabled: Boolean? = true

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
  ): FeatureValue {
    if (!person.hasWriteRole()) {
      val env = QDbEnvironment().id.eq(eid).whenArchived.isNull.findOne()
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

      if (dbFeatureValue.rolloutStrategies == null) { // ensure this is not null for further calculation
        dbFeatureValue.rolloutStrategies = mutableListOf()
      }
      // this is an update not a create, environment + app-feature key exists
      onlyUpdateFeatureValueForEnvironment(featureValue, person, dbFeatureValue)!!
    } else if (person.hasChangeValueRole() || person.hasLockRole() || person.hasUnlockRole()) {
      onlyCreateFeatureValueForEnvironment(eid, key, featureValue, person)!!
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
    val environ = convertUtils.byEnvironment(eid)
    if (environ != null) {
      val appFeature = QDbApplicationFeature().key.eq(key).parentApplication.environments.eq(environ).findOne()
      if (appFeature != null) {
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

  @Transactional(type = TxType.REQUIRES_NEW)
  private fun save(featureValue: DbFeatureValue) {
    saveFeatureValue(featureValue)
  }

  override fun saveFeatureValue(featureValue: DbFeatureValue) {
    database.save(featureValue)

    if (auditingEnabled!!) {
      // now saved a versioned copy
      database.save(DbFeatureValueVersion.fromDbFeatureValue(featureValue))
    }
  }

  private fun publish(featureValue: DbFeatureValue) {
    log.trace("publishing update for {}", featureValue)
    cacheSource.publishFeatureChange(featureValue)
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  override fun deleteFeatureValueForEnvironment(eid: UUID, key: String): Boolean {
    Conversions.nonNullEnvironmentId(eid)
    val strategy = QDbFeatureValue().environment.id.eq(eid).feature.key.eq(key).findOne()
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
    existing: DbFeatureValue
  ): FeatureValue? {
    if (featureValue.version == null) {
      throw OptimisticLockingException() // we cannot determine what version to  compare this against
    }

    if (auditingEnabled!!) {
      val historical = QDbFeatureValueVersion().id.id.eq(featureValue.id).id.version.eq(featureValue.version).findOne()
      if (historical == null && existing.version != featureValue.version) {
        // there is no historical value to compare against and we aren't updating the existing version
        throw OptimisticLockingException()
      }
      if (historical == null) {
        updateFeatureValue(featureValue, person, existing)

        save(existing)
        publish(existing)
      } else {
        // saving is done inside here as it detects it
        updateSelectively(featureValue, person, existing, historical)
      }
    } else {
      updateFeatureValue(featureValue, person, existing)

      save(existing)
      publish(existing)
    }

    return convertUtils.toFeatureValue(existing)
  }

  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class)
  internal fun updateSelectively(
    featureValue: FeatureValue,
    person: PersonFeaturePermission, existing: DbFeatureValue,
    historical: DbFeatureValueVersion
  ) {
    val feature = existing.feature

    val lockUpdate = updateSelectivelyLocked(featureValue, historical, existing, person)
    val lockChanged = lockUpdate.hasChanged

    // allow them to change the value and lock it at the same time
    val defaultValueUpdate =
      updateSelectivelyDefaultValue(feature, featureValue, historical, existing, person, lockChanged)

    val strategyUpdates = updateSelectivelyRolloutStrategies(person, featureValue, historical, existing, lockChanged)

    val retiredUpdate = updateSelectivelyRetired(person, featureValue, historical, existing, lockChanged)

    if (lockChanged || defaultValueUpdate.hasChanged || strategyUpdates.hasChanged || retiredUpdate.hasChanged) {
      existing.whoUpdated = QDbPerson().id.eq(person.person.id!!.id).findOne()
      save(existing)
      publish(existing)
      publishChangesForMessaging(existing, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)
    }
  }

  private fun publishChangesForMessaging(
    featureValue: DbFeatureValue,
    lockUpdate: SingleFeatureValueUpdate<Boolean>,
    defaultValueUpdate: SingleNullableFeatureValueUpdate<String?>,
    retiredUpdate: SingleFeatureValueUpdate<Boolean>,
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>
  ) {
    try {
      val featureMessagingParameter = FeatureMessagingParameter(featureValue, lockUpdate, defaultValueUpdate, retiredUpdate, strategyUpdates)
      featureMessagingCloudEventPublisher.publishFeatureMessagingUpdate(featureMessagingParameter)
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
    val existingRetired = (existing.retired == true) // it can be null, which is also false

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

      existing.retired = featureValue.retired

      val updated = featureValue.retired ?: false
      updateSingleFeatureValueUpdate(retiredFeatureValueUpdate, updated, historical.isRetired)

      return retiredFeatureValueUpdate
    }

    // otherwise they changed it from historical and existing has already changed
    throw OptimisticLockingException()
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

    val historicalStrategies = historical.rolloutStrategies
    val existingStrategies = existing.rolloutStrategies

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
        val historicalStrategy = historicalStrategies?.find { it.id == strategy.id }

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
          addToStrategyUpdates(type = "add", newStrategy = strategy, strategyUpdates = strategyUpdates)
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
            addToStrategyUpdates(type = "change", newStrategy = strategy, oldStrategy = historicalStrategy, strategyUpdates = strategyUpdates)
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
            addToStrategyUpdates(type = "delete", oldStrategy = strategyToDelete, strategyUpdates = strategyUpdates)
          }
      }

      // ok, now just honour the order of the incoming strategies and keep track if they actually changed
      val newlyOrderedList =
        featureValue.rolloutStrategies?.mapNotNull { newStrategy -> existingStrategies.find { it.id == newStrategy.id } }
        ?.toMutableList() ?: mutableListOf()
      val newlyOrderedListIds = newlyOrderedList.map { it.id }
      newlyOrderedList.addAll(existingStrategies.filter { !newlyOrderedListIds.contains(it.id) })
      val reorderedList = newlyOrderedList.map { it.id }

      if (existingStrategies?.map { it.id } != reorderedList) {
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
    type: String, newStrategy: RolloutStrategy? = null, oldStrategy: RolloutStrategy? = null ) {
    strategyUpdates.hasChanged = true
    val rollingStrategyUpdate = RolloutStrategyUpdate(type = type, new = newStrategy, old = oldStrategy)
    strategyUpdates.updated.add(rollingStrategyUpdate)
  }

  private fun addToStrategyReorders(strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>,
    reordered: MutableList<RolloutStrategy>, previous: MutableList<RolloutStrategy>) {
    strategyUpdates.hasChanged = true
    strategyUpdates.reordered = reordered
    strategyUpdates.previous = previous
  }

  private fun <T> updateSingleFeatureValueUpdate(
    featureValueUpdate: SingleFeatureValueUpdate<T>, updated: T, previous: T ): SingleFeatureValueUpdate<T> {
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
      when (feature.valueType!!) {
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


  /**
   * this is the original logic - do not touch, it is also used for the creation of the feature value the first time
   */
  @Throws(FeatureApi.NoAppropriateRole::class)
  private fun updateFeatureValue(
    featureValue: FeatureValue,
    person: PersonFeaturePermission,
    dbFeatureValue: DbFeatureValue
  ) {
    val feature = dbFeatureValue.feature
    if (person.hasChangeValueRole() && (!dbFeatureValue.isLocked || java.lang.Boolean.FALSE == featureValue.locked && person.hasUnlockRole())) {
      when (feature.valueType!!) {
        FeatureValueType.NUMBER -> {
          dbFeatureValue.defaultValue =
            if (featureValue.valueNumber == null) null else featureValue.valueNumber.toString()
        }

        FeatureValueType.STRING -> {
          dbFeatureValue.defaultValue = featureValue.valueString
        }

        FeatureValueType.JSON -> {
          dbFeatureValue.defaultValue = featureValue.valueJson
        }

        FeatureValueType.BOOLEAN -> {
          dbFeatureValue.defaultValue =
            if (featureValue.valueBoolean == null) java.lang.Boolean.FALSE.toString() else featureValue.valueBoolean.toString()
        }
      }

      featureValue.rolloutStrategies?.let { strategies ->
        rationaliseStrategyIdsAndAttributeIds(strategies)
      }

      dbFeatureValue.rolloutStrategies = featureValue.rolloutStrategies
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

  fun rationaliseStrategyIdsAndAttributeIds(strategies: List<RolloutStrategy>) {
    strategies.forEach { strategy ->
      if (strategy.id == null || strategy.id!!.length > strategyIdLength) {
        var id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
        // make sure it is unique
        while (strategies.any { id == strategy.id }) {
          id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
        }

        strategy.id = id
      }

      strategy.attributes?.forEach { attribute ->
        if (attribute.id == null || attribute.id!!.length > strategyIdLength) {
          var id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
          // make sure it is unique
          while (strategy.attributes!!.any { id == attribute.id }) {
            id = RandomStringUtils.randomAlphanumeric(strategyIdLength)
          }

          attribute.id = id
        }
      }
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
  ): FeatureValue {
    return createFeatureValueForEnvironment(eid, key, featureValue, person)
  }

  override fun getFeatureValueForEnvironment(eid: UUID, key: String): FeatureValue? {
    Conversions.nonNullEnvironmentId(eid)
    val featureValue =
      QDbFeatureValue().environment.id.eq(eid).feature.key.eq(key).sharedRolloutStrategies.fetch().findOne()
    return if (featureValue == null) null else convertUtils.toFeatureValue(featureValue)
  }

  override fun getAllFeatureValuesForEnvironment(eid: UUID): EnvironmentFeaturesResult {
    Conversions.nonNullEnvironmentId(eid)
    return EnvironmentFeaturesResult()
      .featureValues(
        QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
          .map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs) }
      )
      .environments(listOf(convertUtils.toEnvironment(QDbEnvironment().id.eq(eid).findOne(), Opts.empty())))
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
    Conversions.nonNullEnvironmentId(eid)
    require(
      featureValues.size == featureValues.map { obj: FeatureValue -> obj.key }.toSet().size
    ) { "Invalid update dataset" }

    val existing = QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
    val existingFeatures =
      QDbApplicationFeature().parentApplication.environments.id.eq(eid).findList().associateBy { it.key }
    val newValues = featureValues.associateBy { it.key }.toMutableMap()

    // ensure the strategies are valid from a conceptual perspective
    val failure = RolloutStrategyValidator.ValidationFailure()
    for (fv in featureValues) {
      if (newValues[fv.key] == null) {
        throw FeatureApi.NoAppropriateRole()
      }

      val feat = existingFeatures[fv.key] ?: throw FeatureApi.NoSuchFeature()

      rolloutStrategyValidator.validateStrategies(
        feat.valueType,
        fv.rolloutStrategies ?: listOf(), fv.rolloutStrategyInstances ?: listOf(), failure
      )
    }
    failure.hasFailedValidation()

    val deleteKeys = existing.map { e: DbFeatureValue -> e.feature.key }.toMutableList()
    // take them all and remove all fv's we were passed, leaving only EFS's we want to remove
    for (fv in featureValues) {
      deleteKeys.remove(fv.key)
    }

    // we should be left with only keys in deleteKeys that do not exist in the passed in list of feature values
    // and in addingKeys we should be given a list of keys which exist in the passed in FV's but didn't exist in the db
    val deleteStrategies = mutableListOf<DbFeatureValue>()
    for (strategy in existing) {
      if (deleteKeys.contains(strategy.feature.key)) {
        if (strategy.feature.valueType != FeatureValueType.BOOLEAN) {
          deleteStrategies.add(strategy) // can't delete booleans
        }
      } else {
        val fv = newValues.remove(strategy.feature.key)
        if (fv != null) {
          onlyUpdateFeatureValueForEnvironment(fv, requireRoleCheck, strategy)
        }
      }
    }

    // now for the creates
    for (key in newValues.keys) {
      newValues[key]?.let { fv ->
        onlyCreateFeatureValueForEnvironment(eid, key, fv, requireRoleCheck)
      }

    }
    if (deleteStrategies.isNotEmpty()) {
      publishTheRemovalOfABunchOfStrategies(deleteStrategies)
    }

    return QDbFeatureValue().environment.id.eq(eid).feature.whenArchived.isNull.findList()
      .map { fs: DbFeatureValue -> convertUtils.toFeatureValue(fs)!! }
  }

  // can't background this because they will deleted shortly
  @Transactional(type = TxType.REQUIRES_NEW)
  private fun publishTheRemovalOfABunchOfStrategies(deleteStrategies: Collection<DbFeatureValue?>) {
    if (!deleteStrategies.isEmpty()) {
      deleteStrategies.parallelStream().forEach { strategy: DbFeatureValue? ->
        cacheSource.deleteFeatureChange(
          strategy!!.feature, strategy.environment.id
        )
      }

      database.deleteAll(deleteStrategies)
    }
  }

  @Throws(RolloutStrategyValidator.InvalidStrategyCombination::class)
  override fun updateFeature(
    sdkUrl: String, envId: UUID, featureKey: String, updatingValue: Boolean,
    buildFeatureValue: Function<FeatureValueType, FeatureValue>
  ) {
    Conversions.nonNullEnvironmentId(envId)

    // not checking permissions, edge checks those
    val feature = QDbApplicationFeature().parentApplication.environments.id.eq(envId).key.eq(featureKey)
      .findOne()
      ?: return
    var fv = QDbFeatureValue().environment.id.eq(envId).feature.eq(feature).findOne()
    val newValue = buildFeatureValue.apply(feature.valueType)
    rolloutStrategyValidator.validateStrategies(
      feature.valueType,
      newValue.rolloutStrategies ?: listOf(),
      newValue.rolloutStrategyInstances ?: listOf()
    ).hasFailedValidation()
    val saveNew = fv == null
    if (saveNew) { // creating
      fv = DbFeatureValue.Builder()
        .environment(QDbEnvironment().id.eq(envId).findOne())
        .feature(feature)
        .locked(true)
        .build()
    }
    if (updatingValue) {
      when (fv!!.feature.valueType!!) {
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

  internal class EnvironmentsAndFeatureValues(
    // this is envId -> DbFeatureValue
    var envIdToDbFeatureValue: MutableMap<UUID, DbFeatureValue>,
    val feature: DbApplicationFeature,
    var envIdToRolePermissions: Map<UUID, List<RoleType>>,
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

    val featureValuesResult: MutableMap<UUID, DbFeatureValue> = mutableMapOf()
    val roles: MutableMap<UUID, List<RoleType>> = mutableMapOf()
    val environments: MutableMap<UUID, DbEnvironment> = mutableMapOf()
    val personAdmin = convertUtils.isPersonApplicationAdmin(person.id!!.id, appId)
    // this can be empty as the feature may not have a value in this environment if it is a non-bool
    val featureValueList = QDbFeatureValue()
      .feature.key.eq(key)
      .feature.parentApplication.id.eq(appId)
      .findList()
    val envIdToDbFeatureValues = featureValueList
      .associateBy { it.environment.id }.toMutableMap()

    val adminRoles = listOf(*RoleType.values())
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
            roles[envId] = roleTypes
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
          roles[env.id] = if (personAdmin) adminRoles else emptyRoles
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
    Conversions.nonNullApplicationId(appId)
    val result = featureValuesUserCanAccess(appId, key, person)

    return result.environments.keys.map { e ->
      convertUtils.toFeatureEnvironment(
        result.envIdToDbFeatureValue[e], result.envIdToRolePermissions[e]!!,
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
    removeValuesNotPassed: Boolean
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
    val featureValuesToDelete = result.envIdToDbFeatureValue
    for (fv in featureValue) {
      createFeatureValueForEnvironment(
        fv.environmentId!!, key, fv,
        PersonFeaturePermission.Builder()
          .person(from)
          .appRoles(result.appRolesForThisPerson)
          .roles(HashSet(environmentToRoleMap[fv.environmentId!!]!!)).build()
      )

      featureValuesToDelete.remove(fv.environmentId) // we processed this environment ok, didn't throw a wobbly
    }

    // now remove any ability to remove feature values that are flags
    val invalidDeletions = featureValuesToDelete.keys
      .filter { u: UUID -> featureValuesToDelete[u]!!.feature.valueType != FeatureValueType.BOOLEAN }

    invalidDeletions.forEach { featureValuesToDelete.remove(it) }

    if (removeValuesNotPassed) {
      publishTheRemovalOfABunchOfStrategies(featureValuesToDelete.values)
    }
  }

  private fun environmentToFeatureValues(acl: DbAcl, personIsAdmin: Boolean, featureKeys: List<String>): EnvironmentFeatureValues? {
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
      .features(featuresForEnvironment(acl.environment, featureKeys))
  }

  private fun featuresForEnvironment(environment: DbEnvironment, featureKeys: List<String>): List<FeatureValue> {
    val featureValueFinder = QDbFeatureValue()
      .environment.eq(environment)
      .environment.whenArchived.isNull
      .feature.whenArchived.isNull
      .feature.key.`in`(featureKeys)

    return featureValueFinder.findList().map { fs: DbFeatureValue? -> convertUtils.toFeatureValue(fs)!! }
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
    sortOrder: SortOrder?
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

      if (sort == SortOrder.ASC) {
        appFeatureQuery = appFeatureQuery.order().key.asc()
      } else {
        appFeatureQuery = appFeatureQuery.order().key.desc()
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

      val featureKeys = features.map { f -> f.key!! }

      // because we need to know what features there are on what pages, we grab the application features we are going
      // to use *first*
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
          .group.groupMembers.person.eq(
            dbPerson
          ).findList()
          .onEach { acl: DbAcl -> environmentOrderingMap[acl.environment.id] = acl.environment }
          .mapNotNull { acl: DbAcl -> environmentToFeatureValues(acl, personAdmin, featureKeys) }
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
            original.features!!.map { obj: FeatureValue -> obj.id }.toSet()
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
        val roles = if (personAdmin) listOf(*RoleType.values()) else listOf()
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
        ArrayList(QDbEnvironment().select(QDbEnvironment.Alias.id).parentApplication.id.eq(appId).findList())
      EnvironmentUtils.sortEnvironments(sortingEnvironments)
      val finalValues: MutableList<EnvironmentFeatureValues> = ArrayList()
      sortingEnvironments.forEach { e: DbEnvironment ->
        val efv = envs[e.id]
        if (efv != null) {
          finalValues.add(efv)
        }
      }

      return ApplicationFeatureValues()
        .applicationId(appId)
        .features(features)
        .environments(finalValues)
        .maxFeatures(totalFeatureCount)
    }
    return null
  }

  // it is already in a transaction for the job table, so it needs a new one
  @Transactional(type = TxType.REQUIRES_NEW)
  override fun release1_5_11_strategy_update() {
    if (auditingEnabled == false) {
      return
    }

    log.info(
      "1.5.11 requires updating of the internal structure of strategies and the creation of a historical record, there are {} to process",
      QDbFeatureValue().findCount()
    )

    var count = 0

    QDbFeatureValue().findList().forEach { fv ->
      count++

      fv.rolloutStrategies?.let { strategies ->
        rationaliseStrategyIdsAndAttributeIds(strategies)
      }

      save(fv)

      if (count % 50 == 0) {
        log.info("upgraded {} feature values", count)
      }
    }

    log.info("feature value upgrade complete")
  }

  companion object {
    private val log = LoggerFactory.getLogger(FeatureSqlApi::class.java)

    const val strategyIdLength = 4
  }
}
