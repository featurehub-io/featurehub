package io.featurehub.events

import io.featurehub.utils.FallbackPropertyConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface DynamicCloudEventDestination {
  val destination: String
  val headers: Map<String, String>
  val cloudEventType: String
  fun additionalProperties(sourceWebhookMap: Map<String, String>): Map<String, String>
}
/**
 * The purpose of this type is to associate a cloud event type with configuration provided by the person who is running
 * FeatureHub. It goes and tells the system using the dynamic publishers to register a listener for a cloud event of type X
 * against a publisher of type Y. Extra headers can be provided for some transport mechanisms (currently only http/https)
 */
class DynamicCloudEventDestinationMapper(
  override val cloudEventType: String, private val prefix: String,
  private val configInfix: String, dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry
) : DynamicCloudEventDestination {
  // used to determine where to send this
  override val destination = FallbackPropertyConfig.getMandatoryConfig("${prefix}.destination")

  // used to automatically add to each outgoing CE message
  override val headers =
    FallbackPropertyConfig.getConfig("${prefix}.headers", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
      .map {
        if (it.contains("=")) {
          val splits = it.split("=")
          splits[0] to splits[1]
        } else { // allow for secrets
          it to FallbackPropertyConfig.getConfig("${prefix}.headers.${it}", "")
        }
      }.toMap()

  init {
    // this will create a new publisher based on config for this type
    dynamicPublisherRegistry.requireDynamicPublisher(destination, headers, cloudEventType)

    log.info("registered new dynamic publisher $configInfix for destination $destination for CE-type of $cloudEventType (headers $headers)")
  }

  // augumented into the body of the message
  override fun additionalProperties(sourceWebhookMap: Map<String, String>): Map<String, String> {
    return sourceWebhookMap
      .filter { it.key.startsWith(configInfix) }
      .filter { it.key != "$prefix.enabled" }
      .map { it.key to it.value }.toMap()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(DynamicCloudEventDestinationMapper::class.java)

    fun infix(infix: String, dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry): String? {
      if (FallbackPropertyConfig.getConfig("webhook.${infix}.enabled", "false") == "true") {
        FallbackPropertyConfig.getConfig("webhook.${infix}.destination")?.let {
          if (dynamicPublisherRegistry.confirmDynamicPublisherExists(it)) {
            return "webhook.${infix}"
          }
        }

        FallbackPropertyConfig.getConfig("webhook.default.destination")?.let {
          if (dynamicPublisherRegistry.confirmDynamicPublisherExists(it)) {
            return "webhook.default"
          }
        }
      }

      return null
    }
  }
}
