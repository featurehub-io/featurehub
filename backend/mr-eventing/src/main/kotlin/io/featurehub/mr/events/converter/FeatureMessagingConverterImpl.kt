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
    lockUpdate: SingleFeatureValueUpdate<Boolean>?,
    defaultValueUpdate: SingleFeatureValueUpdate<String>?,
    retiredUpdate: SingleFeatureValueUpdate<Boolean>?,
    strategyUpdates: MultiFeatureValueUpdate<RolloutStrategyUpdate, RolloutStrategy>?
  ): FeatureMessagingUpdate {

    val environment = featureValue.environment
    val parentApplication = environment.parentApplication
    val portfolio = parentApplication.portfolio
    return FeatureMessagingUpdate()
      .featureKey(featureValue.feature.key)
      .environmentId(environment.id)
      .whoUpdated(featureValue.whoUpdated.name)
      .whenUpdated(featureValue.whenUpdated.atOffset(ZoneOffset.UTC))
      .applicationId(parentApplication.id)
      .portfolioId(portfolio.id)
      .organizationId(portfolio.organization.id)
      .let {
        val defaultValueUpdated = defaultValueUpdate?.updated
        val defaultValuePrevious = defaultValueUpdate?.previous
        if (defaultValueUpdate?.hasChanged == true && defaultValueUpdated != null && defaultValuePrevious != null) it.featureValueUpdated(
          MessagingFeatureValueUpdate()
            .valueType(featureValue.feature.valueType)
            .updated(defaultValueUpdated)
            .previous(defaultValuePrevious)
        )
        else it
      }
      .let {
        val lockUpdated = lockUpdate?.updated
        val lockPrevious = lockUpdate?.previous
        if (lockUpdate?.hasChanged == true && lockUpdated != null && lockPrevious != null)
          it.lockUpdated(MessagingLockUpdate()
            .updated(lockUpdated)
            .previous(lockPrevious)
        ) else it
      }
      .let {
        val retiredUpdated = retiredUpdate?.updated
        val retiredPrevious = retiredUpdate?.previous
        if (retiredUpdate?.hasChanged == true && retiredUpdated != null && retiredPrevious != null)
          it.retiredUpdated(MessagingRetiredUpdate()
            .updated(retiredUpdated)
            .previous(retiredPrevious)
        ) else it
      }
      .let {
        val messagingStrategiesReorder = MessagingStrategiesReorder()
        if (strategyUpdates?.hasChanged == true && strategyUpdates.updated.isNotEmpty())
          it.strategiesUpdated(strategyUpdates.updated.map { rolloutStrategyUpdate ->  toMessagingStrategyUpdate(rolloutStrategyUpdate) })
        if (strategyUpdates?.hasChanged == true && strategyUpdates.reordered.isNotEmpty())
          it.strategiesReordered(
            messagingStrategiesReorder.reordered(
          strategyUpdates.reordered.map { rolloutStrategy -> toMessagingRolloutStrategy(rolloutStrategy) }
        ))
        if (strategyUpdates?.hasChanged == true && strategyUpdates.previous.isNotEmpty())
          it.strategiesReordered(
            messagingStrategiesReorder.previous(
            strategyUpdates.previous.map { rolloutStrategy -> toMessagingRolloutStrategy(rolloutStrategy) }
          ))
        else it
      }

  }

  private fun toMessagingStrategyUpdate(rolloutStrategyUpdate: RolloutStrategyUpdate): MessagingStrategyUpdate {
    val new = rolloutStrategyUpdate.new
    val old = rolloutStrategyUpdate.old
    return MessagingStrategyUpdate()
      .let { if (new != null) it.newStrategy(toMessagingRolloutStrategy(new)) else it }
      .let { if (old != null) it.oldStrategy(toMessagingRolloutStrategy(old)) else it }
      .updateType(StrategyUpdateType.fromValue(rolloutStrategyUpdate.type))
  }

  private fun toMessagingRolloutStrategy(rolloutStrategy: RolloutStrategy): MessagingRolloutStrategy {
    val attributes = rolloutStrategy.attributes
    return MessagingRolloutStrategy()
      .id(rolloutStrategy.id ?: "rs-id")
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .value(rolloutStrategy.value)
      .attributes(attributes?.map { rsa: RolloutStrategyAttribute -> toRolloutStrategyAttribute(rsa) } ?: ArrayList())
  }

  private fun toRolloutStrategyAttribute(rolloutStrategyAttribute: RolloutStrategyAttribute): MessagingRolloutStrategyAttribute {
    return MessagingRolloutStrategyAttribute()
      .conditional(rolloutStrategyAttribute.conditional!!)
      .values(rolloutStrategyAttribute.values)
      .fieldName(rolloutStrategyAttribute.fieldName!!)
      .type(rolloutStrategyAttribute.type!!)
  }

}
