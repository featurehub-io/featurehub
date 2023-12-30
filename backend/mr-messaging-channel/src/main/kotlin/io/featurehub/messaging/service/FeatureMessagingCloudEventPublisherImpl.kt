package io.featurehub.messaging.service

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.db.api.RolloutStrategyUpdate
import io.featurehub.db.messaging.FeatureMessagingParameter
import io.featurehub.db.messaging.FeatureMessagingPublisher
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisher
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.events.DynamicCloudEventDestinationMapper
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
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


@LifecyclePriority(priority = 12) // this is after all the dynamic registries have registered (they register at priority 5)
class FeatureMessagingCloudEventInitializer @Inject constructor(publisher: FeatureMessagingCloudEventPublisher,
                                                          dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry) : LifecycleListener {
  init {
    val hooks = mapOf(Pair("integration.slack", "integration/slack-v1")).map {
      val prefix = DynamicCloudEventDestinationMapper.infix(it.key, dynamicPublisherRegistry)
      if (prefix != null) DynamicCloudEventDestinationMapper(it.value, prefix, it.key, dynamicPublisherRegistry) else null
    }.filterNotNull()

    publisher.setHooks(hooks)
  }
}

open class FeatureMessagingCloudEventPublisherImpl @Inject constructor(
  private val cloudEventPublisher: CloudEventPublisher,
  private val supplier: ExecutorSupplier
) : FeatureMessagingCloudEventPublisher, FeatureMessagingPublisher {
  private val log: Logger = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)
  private val hooks = mutableListOf<DynamicCloudEventDestination>();
  private var executor: ExecutorService? = null

  override fun setHooks(hooks: List<DynamicCloudEventDestination>) {
    this.hooks.clear()
    this.hooks.addAll(hooks)

    if (hooks.isNotEmpty()) {
      executor = supplier.executorService(FallbackPropertyConfig.getConfig("messaging.publish.thread-pool", "4").toInt())
    }
  }

  override val isEnabled: Boolean
    get() = hooks.isNotEmpty()

  override fun publish(featureMessagingParameter: FeatureMessagingParameter) {
    if (hooks.isEmpty()) return // skip as we have no enabled hooks

    executor?.let {
      it.submit {
        try {
          val featureMessagingUpdate = toFeatureMessagingUpdate(featureMessagingParameter)
          val webhookEnvironmentInfo = featureMessagingParameter.featureValue.environment.webhookEnvironmentInfo ?: mapOf()
          val messageId = UUID.randomUUID().toString()

          hooks.forEach { hook ->
            log.trace("publishing feature messaging update for {}", featureMessagingUpdate)
            val event = CloudEventBuilder.v1().newBuilder()
            event.withSubject(FeatureMessagingUpdate.CLOUD_EVENT_SUBJECT)
            // internal type
            event.withExtension("originatingtype", FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
            event.withId(messageId)
            event.withSource(URI("http://mr"))
            event.withTime(OffsetDateTime.now())

            // this is all the config info from the webhook, which can include encrypted data
            featureMessagingUpdate.additionalInfo = hook.additionalProperties(webhookEnvironmentInfo)

            cloudEventPublisher.publish(hook.cloudEventType, featureMessagingUpdate, event)
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
        .portfolioName(portfolio.name)
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
