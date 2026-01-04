package io.featurehub.db.services

import io.ebean.annotation.Transactional
import io.ebean.annotation.TxType
import io.featurehub.db.api.FeatureApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.PersonFeaturePermission
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.model.SharedRolloutStrategyVersion
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbApplicationRolloutStrategy
import io.featurehub.db.model.query.QDbFeatureValueVersion
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.FeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyInstance
import jakarta.inject.Inject
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface UpdateFeatureApi {
  fun onlyCreateFeatureValueForEnvironment(
    eid: UUID, key: String, featureValue: FeatureValue,
    person: PersonFeaturePermission
  ): FeatureValue?

  fun onlyUpdateFeatureValueForEnvironment(
    featureValue: FeatureValue,
    person: PersonFeaturePermission,
    existing: DbFeatureValue,
    changingDefaultValue: Boolean,
    updatingLock: Boolean,
  ): FeatureValue?

  fun rationaliseStrategyIdsAndAttributeIds(strategies: List<RolloutStrategy>): Boolean

  /**
   * delegates to internalFeatureApi in a new transaction
   */
  fun save(featureValue: DbFeatureValue, forceUpdate: Boolean = false)
}

class UpdateFeatureApiImpl@Inject constructor(
  private val convertUtils: Conversions,
  private val cacheSource: CacheSource,
  private val featureMessagePublisher: FeatureMessagingPublisher,
) : UpdateFeatureApi {
  private val internalFeatureApi: InternalFeatureApi =
    InternalFeatureSqlApi(convertUtils, cacheSource, featureMessagePublisher)

  @Throws(OptimisticLockingException::class, FeatureApi.NoAppropriateRole::class)
  override fun onlyUpdateFeatureValueForEnvironment(
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

  @Throws(FeatureApi.NoAppropriateRole::class)
  override fun onlyCreateFeatureValueForEnvironment(
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
  override fun save(featureValue: DbFeatureValue, forceUpdate: Boolean) {
    internalFeatureApi.saveFeatureValue(featureValue, forceUpdate)
  }

  private fun publish(featureValue: DbFeatureValue) {
    log.trace("publishing update for {}", featureValue)
    cacheSource.publishFeatureChange(featureValue)
  }


  private fun convertStrategiesToDbFeatureValueStrategies(featureValue: FeatureValue): List<RolloutStrategy> {
    return featureValue.rolloutStrategies?.let { strategies ->
      rationaliseStrategyIdsAndAttributeIds(strategies)
      strategies
    } ?: listOf()
  }

  override fun rationaliseStrategyIdsAndAttributeIds(strategies: List<RolloutStrategy>): Boolean {
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


  companion object {
    val log: Logger = LoggerFactory.getLogger(UpdateFeatureApiImpl::class.java)

    const val strategyIdLength = 4
  }
}
