package io.featurehub.db.api

import io.featurehub.mr.model.*
import java.util.*

class LockedException : RuntimeException()

interface FeatureApi {
    @Throws(OptimisticLockingException::class, NoAppropriateRole::class, RolloutStrategyValidator.InvalidStrategyCombination::class, LockedException::class)
    fun updateAllFeatureValuesByApplicationForKey(
        id: UUID, key: String, featureValue: List<FeatureValue>,
        from: Person,
        removeValuesNotPassed: Boolean
    )

    fun findAllFeatureAndFeatureValuesForEnvironmentsByApplication(
        appId: UUID,
        current: Person
    ): ApplicationFeatureValues?

    class NoAppropriateRole : Exception()

    @Throws(OptimisticLockingException::class, RolloutStrategyValidator.InvalidStrategyCombination::class, NoAppropriateRole::class, LockedException::class)
    fun createFeatureValueForEnvironment(
        eid: UUID, key: String, featureValue: FeatureValue,
        person: PersonFeaturePermission
    ): FeatureValue?

    fun deleteFeatureValueForEnvironment(eid: UUID, key: String): Boolean

    @Throws(OptimisticLockingException::class, RolloutStrategyValidator.InvalidStrategyCombination::class, NoAppropriateRole::class, LockedException::class)
    fun updateFeatureValueForEnvironment(
        eid: UUID, key: String, featureValue: FeatureValue,
        person: PersonFeaturePermission
    ): FeatureValue?

    fun getFeatureValueForEnvironment(eid: UUID, key: String): FeatureValue?
    fun getAllFeatureValuesForEnvironment(eid: UUID): EnvironmentFeaturesResult?

    @Throws(OptimisticLockingException::class, NoAppropriateRole::class, RolloutStrategyValidator.InvalidStrategyCombination::class, LockedException::class)
    fun updateAllFeatureValuesForEnvironment(
        eid: UUID, featureValues: List<FeatureValue>,
        requireRoleCheck: PersonFeaturePermission
    ): List<FeatureValue?>?

    fun getFeatureValuesForApplicationForKeyForPerson(
        appId: UUID,
        key: String,
        person: Person
    ): List<FeatureEnvironment?>?
}
