package io.featurehub.mr.events.converter

import io.featurehub.db.api.MultiFeatureValueUpdate
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.SingleFeatureValueUpdate
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.messaging.model.*
import io.featurehub.mr.events.common.converter.FeatureMessagingConverter
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import java.time.ZoneOffset
import java.util.ArrayList

class FeatureMessagingConverterImpl : FeatureMessagingConverter{
  override fun toFeatureMessagingUpdate(
    featureValue: DbFeatureValue,
    lockUpdate: SingleFeatureValueUpdate<Boolean>,
    defaultValueUpdate: SingleFeatureValueUpdate<String>,
    retiredUpdate: SingleFeatureValueUpdate<Boolean>,
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>
  ): FeatureMessagingUpdate {

    return FeatureMessagingUpdate()
      .featureKey(featureValue.feature.key)
      .environmentId(featureValue.environment.id)
      .whoUpdated(featureValue.whoUpdated.name)
      .whenUpdated(featureValue.whenUpdated.atOffset(ZoneOffset.UTC))
      .applicationId(featureValue.environment.parentApplication.id)
      .let {
        if (defaultValueUpdate.hasChanged) it.featureValueUpdated(
          MessagingFeatureValueUpdate()
            .updated(defaultValueUpdate.updated)
            .previous(defaultValueUpdate.previous)
        )
        else it
      }
      .let {
        if (lockUpdate.hasChanged) it.lockUpdated(
          MessagingLockUpdate()
            .updated(lockUpdate.updated!!)
            .previous(lockUpdate.previous!!)
        ) else it
      }
      .let {
        if (retiredUpdate.hasChanged) it.retiredUpdated(
          MessagingRetiredUpdate()
            .updated(retiredUpdate.updated!!)
            .previous(retiredUpdate.previous!!)
        ) else it
      }
      .let {
        if (strategyUpdates.hasChanged && strategyUpdates.updated.isNotEmpty())
          it.strategiesUpdated(strategyUpdates.updated.map { rolloutStrategyUpdate ->  toMessaginStrategyUpdate(rolloutStrategyUpdate) })
        if (strategyUpdates.hasChanged && strategyUpdates.reordered.isNotEmpty())
          it.strategiesReordered(MessagingStrategiesReorder().reordered(
            strategyUpdates.reordered.map { rolloutStrategy -> toMessagingRolloutStrategy(rolloutStrategy) }
          ))
        else it
      }

  }

  override fun toMessaginStrategyUpdate(rolloutStrategyUpdate: RolloutStrategyUpdate): MessagingStrategyUpdate {
    val new = rolloutStrategyUpdate.new
    val old = rolloutStrategyUpdate.old
    return MessagingStrategyUpdate()
      .let { if (new != null) it.newStrategy(toMessagingRolloutStrategy(new)) else it }
      .let { if (old != null) it.oldStrategy(toMessagingRolloutStrategy(old)) else it }
      .updateType(StrategyUpdateType.fromValue(rolloutStrategyUpdate.type))
  }

  override fun toMessagingRolloutStrategy(rolloutStrategy: RolloutStrategy): MessagingRolloutStrategy {
    val attributes = rolloutStrategy.attributes
    return MessagingRolloutStrategy()
      .id(rolloutStrategy.id ?: "rs-id")
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .value(rolloutStrategy.value)
      .attributes(attributes?.map { rsa: RolloutStrategyAttribute -> toRolloutStrategyAttribute(rsa) } ?: ArrayList())
  }

  override fun toRolloutStrategyAttribute(rolloutStrategyAttribute: RolloutStrategyAttribute): MessagingRolloutStrategyAttribute {
    return MessagingRolloutStrategyAttribute()
      .conditional(rolloutStrategyAttribute.conditional!!)
      .values(rolloutStrategyAttribute.values)
      .fieldName(rolloutStrategyAttribute.fieldName!!)
      .type(rolloutStrategyAttribute.type!!)
  }

}
