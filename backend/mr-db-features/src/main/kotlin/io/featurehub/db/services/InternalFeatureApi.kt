package io.featurehub.db.services

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.api.SingleNullableFeatureValueUpdate
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.db.model.DbApplicationRolloutStrategy
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.DbPerson
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.model.RolloutStrategy
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

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
