package io.featurehub.messaging.converter

import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.messaging.MessagingConfig
import io.featurehub.messaging.model.*
import io.featurehub.messaging.service.FeatureMessagingCloudEventPublisher
import io.featurehub.messaging.service.MappedSupportedConfig
import io.featurehub.mr.model.FeatureVersion
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import java.util.*

class FeatureMessagingConverterImpl @Inject constructor(
  private val messageConfig: MessagingConfig,
  private val publisher: FeatureMessagingCloudEventPublisher
) : FeatureMessagingConverter{

  override fun publish(featureMessagingParameter: FeatureMessagingParameter) {
    if (messageConfig.enabled) {
      val webhookUsage = featureMessagingParameter.featureValue.environment.webhookEnvironmentInfo ?: mapOf()

      publisher.publishFeatureMessagingUpdate(webhookUsage) { -> toFeatureMessagingUpdate(featureMessagingParameter) }
    }
  }

  override fun toFeatureMessagingUpdate(
    featureMessagingParameter: FeatureMessagingParameter
  ): FeatureMessagingUpdate {
    val logger = LoggerFactory.getLogger(FeatureMessagingConverter::class.java)
    try {
      val featureValue = featureMessagingParameter.featureValue
      val environment = featureValue.environment
      val parentApplication = environment.parentApplication
      val portfolio = parentApplication.portfolio
      return FeatureMessagingUpdate()
        .featureKey(featureValue.feature.key)
        .featureId(featureValue.feature.id)
        .featureName(featureValue.feature.name)
        .featureValueId(featureValue.id!!)
        .environmentName(environment.name)
        .environmentId(environment.id)
        .whoUpdatedId(featureValue.whoUpdated.id ?: UUID(0,0))
        .whoUpdated(featureValue.whoUpdated.name ?: "")
        .whenUpdated(featureValue.whenUpdated.atOffset(ZoneOffset.UTC))
        .applicationId(parentApplication.id)
        .appName(parentApplication.name)
        .portfolioId(portfolio.id)
        .portfolioName(portfolio.name)
        .organizationId(portfolio.organization.id)
        .orgName(portfolio.organization.name)
        .version(FeatureVersion().curr(featureMessagingParameter.versionUpdate.updated!!).prev(featureMessagingParameter.versionUpdate.previous))
        .featureValueType(featureValue.feature.valueType)
        .let {
          val defaultValueUpdate = featureMessagingParameter.defaultValueUpdate
          val defaultValueUpdated = defaultValueUpdate.updated
          val defaultValuePrevious = defaultValueUpdate.previous
          if (defaultValueUpdate.hasChanged) it.featureValueUpdated(
            MessagingFeatureValueUpdate()
              .updated(defaultValueUpdated)
              .previous(defaultValuePrevious)
          )
          else it
        }
        .let {
          val lockUpdated = featureMessagingParameter.lockUpdate.updated
          val lockPrevious = featureMessagingParameter.lockUpdate.previous
          if (featureMessagingParameter.lockUpdate.hasChanged)
            it.lockUpdated(MessagingLockUpdate()
              .updated(lockUpdated)
              .previous(lockPrevious)
            ) else it
        }
        .let {
          val retiredUpdated = featureMessagingParameter.retiredUpdate.updated
          val retiredPrevious = featureMessagingParameter.retiredUpdate.previous
          if (featureMessagingParameter.retiredUpdate.hasChanged)
            it.retiredUpdated(MessagingRetiredUpdate()
              .updated(retiredUpdated)
              .previous(retiredPrevious)
            ) else it
        }
        .let {
          val messagingStrategiesReorder = MessagingStrategiesReorder()
          val strategyUpdates = featureMessagingParameter.strategyUpdates
          if (strategyUpdates.hasChanged) {
            if (strategyUpdates.updated.isNotEmpty())
              it.strategiesUpdated(
                strategyUpdates.updated.map { rolloutStrategyUpdate -> toMessagingStrategyUpdate(rolloutStrategyUpdate) })

            if (strategyUpdates.reordered.isNotEmpty())
              it.strategiesReordered(
                messagingStrategiesReorder.reordered(
                  strategyUpdates.reordered.map { rolloutStrategy -> toMessagingRolloutStrategy(rolloutStrategy) }
                ))

            if (strategyUpdates.previous.isNotEmpty())
              it.strategiesReordered(
                messagingStrategiesReorder.previous(
                  strategyUpdates.previous.map { rolloutStrategy -> toMessagingRolloutStrategy(rolloutStrategy) }
                ))
          }
          it
        }
    } catch (e: Exception) {
      logger.error("Unable to convert feature messaging parameter {}",featureMessagingParameter, e)
      throw(e)
    }
  }

  private fun toMessagingStrategyUpdate(rolloutStrategyUpdate: RolloutStrategyUpdate): MessagingStrategyUpdate {
    val new = rolloutStrategyUpdate.new
    val old = rolloutStrategyUpdate.old
    return MessagingStrategyUpdate()
      .let { if (new != null) it.newStrategy(toMessagingRolloutStrategy(new)) else it }
      .let { if (old != null) it.oldStrategy(toMessagingRolloutStrategy(old)) else it }
      .updateType(StrategyUpdateType.fromValue(rolloutStrategyUpdate.type.uppercase())!!)
  }

  private fun toMessagingRolloutStrategy(rolloutStrategy: RolloutStrategy): MessagingRolloutStrategy {
    val attributes = rolloutStrategy.attributes
    return MessagingRolloutStrategy()
      .id(rolloutStrategy.id!!)
      .name(rolloutStrategy.name)
      .percentage(rolloutStrategy.percentage)
      .percentageAttributes(rolloutStrategy.percentageAttributes)
      .value(rolloutStrategy.value)
      .attributes(attributes?.map { rsa: RolloutStrategyAttribute -> toRolloutStrategyAttribute(rsa) } ?: ArrayList())
  }

  private fun toRolloutStrategyAttribute(rolloutStrategyAttribute: RolloutStrategyAttribute): MessagingRolloutStrategyAttribute {
    return MessagingRolloutStrategyAttribute()
      .conditional(rolloutStrategyAttribute.conditional)
      .values(rolloutStrategyAttribute.values)
      .fieldName(rolloutStrategyAttribute.fieldName)
      .type(rolloutStrategyAttribute.type)
  }

}
