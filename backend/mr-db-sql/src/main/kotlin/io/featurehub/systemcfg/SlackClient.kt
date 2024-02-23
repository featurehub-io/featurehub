package io.featurehub.systemcfg

import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.db.services.InternalSystemConfigApi
import io.featurehub.db.services.SystemConfigChange
import io.featurehub.events.CloudEventDynamicDeliveryDetails
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.DynamicCloudEventDestination
import io.featurehub.events.messaging.AdditionalInfoMessage
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.messaging.service.FeatureMessagingPublisherConfiguration
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

interface SlackConfigApi {
  fun registerForPossibleNotifications()
}

class SlackConfig @Inject constructor(
  val fmPublisher: FeatureMessagingPublisherConfiguration,
  private val publisherRegistry: CloudEventDynamicPublisherRegistry,
  private val internalSystemConfigApi: InternalSystemConfigApi
) : KnownSystemConfigSource, SlackConfigApi, DynamicCloudEventDestination {
  private val log: Logger = LoggerFactory.getLogger(SlackConfig::class.java)
  private val slackDeliveryUrl: String? = FallbackPropertyConfig.getConfig("slack.delivery.url")

  companion object {
    val config = listOf(
      ValidSystemConfig(
        "slack.enabled",
        "Whether slack has been configured for this system",
        false,
        KnownSystemConfigSource.boolRef
      ),
      ValidSystemConfig(
        "slack.bearerToken",
        "The system wide bearer token for the FH Slack App",
        true,
        KnownSystemConfigSource.stringRef
      ),
      ValidSystemConfig(
        "slack.messageFormat.featureChange", // format used for feature messaging cloud event
        "The format for Slack messages for Feature Updates",
        false,
        KnownSystemConfigSource.stringRef
      ),
    )

    val deliveryConfig = listOf(
      ValidSystemConfig(
        "slack.delivery.url",
        "The URL of the system which hosts Slack messages (gcp://, nats://, http(s)://",
        false,
        KnownSystemConfigSource.stringRef
      ),
      ValidSystemConfig(
        "slack.delivery.headers",
        "Any headers",
        false,
        KnownSystemConfigSource.mapStringStringRef
      ),
    )

    val mustBeSetConfigs = listOf("slack.enabled", "slack.bearerToken")
    val delivery = listOf("slack.delivery.url", "slack.delivery.headers")
    val info = mapOf(
      Pair("slack.bearerToken", "slack.token"),
      Pair("slack.messageFormat.featureChange", "slack.messageFormat")
    )
  }

  override val knownConfig: List<ValidSystemConfig>
    get() = if (slackDeliveryUrl == null) (config + deliveryConfig) else config

  override fun configUpdateCheck(changes: Map<String, SystemConfigChange>, orgId: UUID) {
  }

  override fun registerForPossibleNotifications() {
    fmPublisher.addHook(this)
  }

  override val cloudEventType: String
    get() = "integration/slack-v1"

  override fun enabled(info: Map<String, String>, orgId: UUID): Boolean {
    val configs = internalSystemConfigApi.findConfigs(mustBeSetConfigs, orgId)

    return configs["slack.enabled"]?.value == true && configs["slack.bearerToken"]?.value != null
  }

  override fun publish(cloudEventType: String, orgId: UUID, data: AdditionalInfoMessage<*>, event: CloudEventBuilder) {
    val configs = internalSystemConfigApi.findConfigs(delivery + info.keys, orgId)

    val map = configs.filter { info.containsKey(it.key) && it.value.value != null }
      .map { Pair(info[it.key]!!, it.value.value!!.toString()) }.toMap().toMutableMap()

    data.additionalInfo?.let { map.putAll(it) }
    data.additionalInfo = map

    val delivery = CloudEventDynamicDeliveryDetails(
      if (slackDeliveryUrl == null) configs["slack.delivery.url"]?.value as String? else slackDeliveryUrl,
      if (slackDeliveryUrl == null) configs["slack.delivery.headers"]?.value as Map<String, String>? else null,
      mapOf(
        Pair("metric.fail.name", "slack_fail"),
        Pair("metric.fail.desc", "Slack publish fails"),
        Pair("metric.histogram.name", "slack_publishes"),
        Pair("metric.histogram.desc", "Slack publishes")
      ),
      true // always send compressed
    )

    if (delivery.isValid()) {
      // we give it to something else to deliver
      publisherRegistry.publish(cloudEventType, data, delivery, event)
    } else {
      log.error("Unable to deliver, server cannot post to Slack directly")
    }
  }
}
