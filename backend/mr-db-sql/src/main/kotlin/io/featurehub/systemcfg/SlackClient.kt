package io.featurehub.systemcfg

import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.db.services.InternalSystemConfigApi
import io.featurehub.db.services.SystemConfigChange
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.events.CloudEventDynamicDeliveryDetails
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.events.messaging.AdditionalInfoMessage
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.messaging.service.FeatureMessagingPublisherConfiguration
import io.featurehub.mr.model.UpdatedSystemConfig
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@LifecyclePriority(priority = 5)
class SlackLifecycleListener @Inject constructor(configApi: SlackConfigApi) : LifecycleListener {
  init {
    configApi.registerForPossibleNotifications()
  }
}

/**
 * We need two of them, one that happens later because by then all of the dynamic configuration has been registered
 * around the place and we can pick up our read only config.
 */
@LifecyclePriority(priority = 100)
class SlackReadOnlyConfigLifecycleListener @Inject constructor(configApi: SlackConfigApi) : LifecycleListener {
  init {
    configApi.registerForReadOnlyConfig()
  }
}

interface SlackConfigApi {
  fun registerForPossibleNotifications()
  fun registerForReadOnlyConfig()
}

class SlackConfig @Inject constructor(
  val fmPublisher: FeatureMessagingPublisherConfiguration,
  private val publisherRegistry: CloudEventDynamicPublisherRegistry,
  private val internalSystemConfigApi: InternalSystemConfigApi
) : KnownSystemConfigSource, SlackConfigApi, DynamicCloudEventDestination {
  private val log: Logger = LoggerFactory.getLogger(SlackConfig::class.java)
  private val slackDeliveryUrl: String? = FallbackPropertyConfig.getConfig("slack.delivery.url")
  private val slackDeliveryInternalOnly: Boolean = FallbackPropertyConfig.getConfig("slack.delivery.internal", "false") == "true"
  private val readOnlyConfig = mutableListOf<ReadOnlySystemConfig>()

  companion object {
    const val cfg_enabled = "slack.enabled"
    const val cfg_bearerToken = "slack.bearerToken"
    const val cfg_defaultChannel = "slack.defaultChannel"
    const val cfg_msgFormatFeatureChange = "slack.messageFormat.featureChange"

    val config = listOf(
      ValidSystemConfig(
        cfg_enabled,
        "Whether slack has been configured for this system",
        false,
        KnownSystemConfigSource.boolRef,
        true, false
      ),
      ValidSystemConfig(
        cfg_bearerToken,
        "The system wide bearer token for the FH Slack App",
        true,
        KnownSystemConfigSource.stringRef,
        true, null
      ),
      ValidSystemConfig(
        cfg_defaultChannel,
        "The default channel if none is provided by the individual environment",
        false,
        KnownSystemConfigSource.stringRef,
        true, null
      ),
      ValidSystemConfig(
        cfg_msgFormatFeatureChange, // format used for feature messaging cloud event
        "The format for Slack messages for Feature Updates",
        false,
        KnownSystemConfigSource.stringRef,
        true, null
      ),
    )
    const val cfg_deliveryUrl = "slack.delivery.url"
    const val cfg_deliveryHeaders = "slack.delivery.headers"

    // this is mutable because it needs to be updated with the header prefixes allowed (which is a read only)
    val deliveryConfig = listOf(
      ValidSystemConfig(
        cfg_deliveryUrl,
        "The URL of the system which hosts Slack messages (gcp://, nats://, http(s)://",
        false,
        KnownSystemConfigSource.stringRef,
        true, null
      ),
      ValidSystemConfig(
        cfg_deliveryHeaders,
        "Any headers",
        true,
        KnownSystemConfigSource.encryptableHeaderRef,
        true, mapOf<String,String>(Pair("slack.encrypted", ""))
      ),
    )

    val allPossibleConfigs = deliveryConfig + config

    val enabledCheckConfig = listOf(cfg_enabled, cfg_bearerToken, cfg_defaultChannel)
    val delivery = listOf(cfg_deliveryUrl, cfg_deliveryHeaders, cfg_defaultChannel)
    val additionalInfoMapping = mapOf(
      Pair(cfg_bearerToken, "slack.token"),
      Pair(cfg_msgFormatFeatureChange, "slack.messageFormat")
    )

    val deliveryMetrics = mapOf(
      Pair("metric.fail.name", "slack_fail"),
      Pair("metric.fail.desc", "Slack publish fails"),
      Pair("metric.histogram.name", "slack_publishes"),
      Pair("metric.histogram.desc", "Slack publishes")
    )
  }

  override val knownConfig: List<ValidSystemConfig>
    get() = if (slackDeliveryUrl == null && !slackDeliveryInternalOnly) (config + deliveryConfig) else config

  override val readOnlyConfg: List<ReadOnlySystemConfig>
    get() = readOnlyConfig

  override fun presaveUpdateCheck(changes: List<UpdatedSystemConfig>, orgId: UUID): String? {
    changes.find { it.key == cfg_deliveryUrl  }?.let { urlChange ->
      val value = if (urlChange.value == null) return "" else urlChange.value.toString()
      if (value.isEmpty()) return null
      if (publisherRegistry.supportedPrefixes.any { value.startsWith(it) }) return null
      return "Invalid prefix used on Slack delivery url"
    }

    return null
  }

  override fun configUpdateCheck(changes: Map<String, SystemConfigChange>, orgId: UUID) {
  }

  override val name: String
    get() = "slack"

  override fun registerForPossibleNotifications() {
    fmPublisher.addHook(this)
  }

  override fun registerForReadOnlyConfig() {
    if (slackDeliveryUrl == null && !slackDeliveryInternalOnly) {
      readOnlyConfig.add(ReadOnlySystemConfig("slack.delivery.prefixes", "Allowable prefixes for delivery urls",
        publisherRegistry.supportedPrefixes))
    }
  }

  override val cloudEventType: String
    get() = "integration/slack-v1"

  override fun enabled(info: Map<String, String>, orgId: UUID): Boolean {
    val configs = internalSystemConfigApi.findConfigs(enabledCheckConfig, orgId, allPossibleConfigs)

    return configs[cfg_enabled]?.value == true && configs[cfg_bearerToken]?.value != null &&
      (info["integration.slack.channel_name"] != null || configs[cfg_defaultChannel]?.value != null) &&
      (info["integration.slack.enabled"] == "true") // the env itself is enabled
  }

  override fun publish(cloudEventType: String, orgId: UUID, data: AdditionalInfoMessage<*>, info: Map<String,String>, event: CloudEventBuilder) {
    val infoHeaders = additionalInfoMapping.keys + SiteConfig.cfg_url
    // this brings in the bearer token
    val configs = internalSystemConfigApi.findConfigs(delivery + infoHeaders, orgId,
      allPossibleConfigs + SiteConfig.config)

    val map = configs.filter { infoHeaders.contains(it.key) && it.value.value != null }
      .map { Pair(additionalInfoMapping[it.key] ?: it.key, it.value.value!!.toString()) }.toMap().toMutableMap()

    val defaultChannel = configs[cfg_defaultChannel]?.value?.toString()
    val definedChannel: String? = info["integration.slack.channel_name"]
    val actualChannel = if (definedChannel.isNullOrBlank()) defaultChannel else definedChannel
    if (actualChannel == null) {
      log.warn("Slack: attempted to publish via slack but no channel defined.")
      return
    }

    map.put("slack.channel", actualChannel)

    data.additionalInfo?.let { map.putAll(it) }
    data.additionalInfo = map

    val delivery = CloudEventDynamicDeliveryDetails(
      if (slackDeliveryUrl == null) configs[cfg_deliveryUrl]?.value as String? else slackDeliveryUrl,
      if (slackDeliveryUrl == null) configs[cfg_deliveryHeaders]?.value as Map<String, String>? else null, // map may contain encrypted values, dest has to decrypt
      deliveryMetrics,
      true // always send compressed
    )

    publisherRegistry.publish("integration/slack-v1", data, delivery, event)
  }
}

