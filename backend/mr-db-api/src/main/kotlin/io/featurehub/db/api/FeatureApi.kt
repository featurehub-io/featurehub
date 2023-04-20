package io.featurehub.db.api

import io.featurehub.mr.model.*
import java.util.*

data class SingleFeatureValueUpdate<T>(var hasChanged: Boolean=false, var updated: T? = null, var previous: T? = null)
data class MultiFeatureValueUpdate<T, V>(var hasChanged: Boolean=false, var updated: MutableList<T> = mutableListOf(), var reordered: MutableList<V> = mutableListOf(), var previous: MutableList<V> = mutableListOf())
data class RolloutStrategyUpdate(val type: String, val old: RolloutStrategy? = null, val new: RolloutStrategy? = null)


interface FeatureApi {
  @Throws(
    OptimisticLockingException::class,
    NoAppropriateRole::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    LockedException::class
  )
  fun updateAllFeatureValuesByApplicationForKey(
    id: UUID, key: String, featureValue: List<FeatureValue>,
    from: Person,
    removeValuesNotPassed: Boolean
  )

  fun findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
    appId: UUID,
    current: Person,
    filter: String?,
    maxFeatures: Int?,
    startingPage: Int?,
    featureValueTypes: List<FeatureValueType>?,
    sortOrder: SortOrder?
  ): ApplicationFeatureValues?

  class NoAppropriateRole : Exception()
  class NoSuchFeature : Exception()
  class LockedException : RuntimeException()

  @Throws(
    OptimisticLockingException::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    NoAppropriateRole::class,
    LockedException::class
  )
  fun createFeatureValueForEnvironment(
    eid: UUID, key: String, featureValue: FeatureValue,
    person: PersonFeaturePermission
  ): FeatureValue?

  fun deleteFeatureValueForEnvironment(eid: UUID, key: String): Boolean

  @Throws(
    OptimisticLockingException::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    NoAppropriateRole::class,
    LockedException::class
  )
  fun updateFeatureValueForEnvironment(
    eid: UUID, key: String, featureValue: FeatureValue,
    person: PersonFeaturePermission
  ): FeatureValue?

  fun getFeatureValueForEnvironment(eid: UUID, key: String): FeatureValue?
  fun getAllFeatureValuesForEnvironment(eid: UUID): EnvironmentFeaturesResult?

  @Throws(
    OptimisticLockingException::class,
    NoAppropriateRole::class,
    RolloutStrategyValidator.InvalidStrategyCombination::class,
    LockedException::class
  )
  fun updateAllFeatureValuesForEnvironment(
    eid: UUID, featureValues: List<FeatureValue>,
    requireRoleCheck: PersonFeaturePermission
  ): List<FeatureValue?>?

  fun getFeatureValuesForApplicationForKeyForPerson(
    appId: UUID,
    key: String,
    person: Person
  ): List<FeatureEnvironment?>?

  /**
   * This specifically upgrades the strategies from 1.5.11 to ensure they all have 4 digit strategies - ready for analytics
   */
  fun release1_5_11_strategy_update()
}
