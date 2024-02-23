package io.featurehub.messaging.service

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.db.api.CloudEventLinkType
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.api.TrackingEventApi
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingFeatureValueUpdate
import io.featurehub.messaging.model.MessagingLockUpdate
import io.featurehub.messaging.model.MessagingRetiredUpdate
import io.featurehub.messaging.model.MessagingRolloutStrategy
import io.featurehub.messaging.model.MessagingRolloutStrategyAttribute
import io.featurehub.messaging.model.MessagingStrategiesReorder
import io.featurehub.messaging.model.MessagingStrategyUpdate
import io.featurehub.messaging.model.StrategyUpdateType
import io.featurehub.mr.model.FeatureVersion
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.ExecutorService


interface FeatureMessagingPublisherConfiguration {
  fun addHook(hook: DynamicCloudEventDestination)
}

open class FeatureMessagingCloudEventPublisherImpl @Inject constructor(
  private val supplier: ExecutorSupplier,
  private val trackingEventApi: TrackingEventApi
) : FeatureMessagingPublisherConfiguration, FeatureMessagingPublisher {
  private val log: Logger = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)
  private val hooks = mutableListOf<DynamicCloudEventDestination>();
  private var executor: ExecutorService? = null

  override fun addHook(hook: DynamicCloudEventDestination) {
    if (hooks.isEmpty()) {
      executor = supplier.executorService(FallbackPropertyConfig.getConfig("messaging.publish.thread-pool", "4").toInt())
    }

    hooks.add(hook)
  }

  override fun publish(featureMessagingParameter: FeatureMessagingParameter, orgId: UUID) {
    val webhookEnvironmentInfo = featureMessagingParameter.featureValue.environment.webhookEnvironmentInfo ?: mapOf()
    if (hooks.isEmpty() || hooks.none { it.enabled(webhookEnvironmentInfo, orgId) } ) return // skip as we have no enabled hooks

    executor?.let {
      it.submit {
        try {
          val featureMessagingUpdate = toFeatureMessagingUpdate(featureMessagingParameter)
          val messageId = UUID.randomUUID()

          for (hook in hooks) {
            if (!hook.enabled(webhookEnvironmentInfo, orgId)) {
              continue
            }

            log.trace("publishing feature messaging update for {}", featureMessagingUpdate)
            val event = CloudEventBuilder.v1().newBuilder()
            event.withSubject(FeatureMessagingUpdate.CLOUD_EVENT_SUBJECT)

            // internal type, CE rules, all lower case
            event.withExtension("originaltype", FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
            event.withExtension("trackedevent", featureMessagingUpdate.organizationId.toString())
            event.withSource(URI("http://mr"))
            event.withId(messageId.toString())
            event.withSource(URI("http://mr"))
            val whenCreated = OffsetDateTime.now()
            event.withTime(whenCreated)

            trackingEventApi.createInitialRecord(messageId, hook.cloudEventType,
              CloudEventLinkType.env, featureMessagingUpdate.environmentId,
              featureMessagingUpdate, whenCreated, null)

            hook.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, orgId, featureMessagingUpdate, event)
          }
        } catch (e: Exception) {
          log.error("Failed to publish messaging update for feature", e)
        }
      }
    }
  }

  open fun toFeatureMessagingUpdate(
    featureMessagingParameter: FeatureMessagingParameter
  ): FeatureMessagingUpdate {
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
        .portfolioName(portfolio.name ?: "")
        .organizationId(portfolio.organization.id)
        .orgName(portfolio.organization.name)
        .version(FeatureVersion().curr(featureMessagingParameter.versionUpdate.updated!!).prev(featureMessagingParameter.versionUpdate.previous))
        .featureValueType(featureValue.feature.valueType)
        .desc(featureValue.feature.description)
        .link(featureValue.feature.link)
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
            it.lockUpdated(
              MessagingLockUpdate()
              .updated(lockUpdated)
              .previous(lockPrevious)
            ) else it
        }
        .let {
          val retiredUpdated = featureMessagingParameter.retiredUpdate.updated
          val retiredPrevious = featureMessagingParameter.retiredUpdate.previous
          if (featureMessagingParameter.retiredUpdate.hasChanged)
            it.retiredUpdated(
              MessagingRetiredUpdate()
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
      log.error("Unable to convert feature messaging parameter {}",featureMessagingParameter, e)
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
