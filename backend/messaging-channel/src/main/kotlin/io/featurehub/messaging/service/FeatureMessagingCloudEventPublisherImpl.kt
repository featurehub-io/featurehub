package io.featurehub.messaging.service

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventPublisher
import io.featurehub.messaging.MessagingConfig
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

class MappedSupportedConfig(val cloudEventType: String, private val prefix: String, private val configInfix: String) {
  // used to determine where to send this
  val destination = FallbackPropertyConfig.getMandatoryConfig("${prefix}.destination")
  // used to automatically add to each outgoing CE message
  val headers =
    FallbackPropertyConfig.getConfig("${prefix}.headers", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.map {
      if (it.contains("=")) {
        val splits = it.split("=")
        splits[0] to splits[1]
      } else { // allow for secrets
        it to FallbackPropertyConfig.getConfig("${prefix}.${it}", "")
      }
    }.toMap()

  // augumented into the body of the message
  fun additionalProperties(sourceWebhookMap: Map<String,String>): Map<String, String> {
    return sourceWebhookMap.filter { it.key.startsWith(prefix) }.map { it.key.substring(configInfix.length) to it.value }.toMap()
  }

  companion object {
    fun prefix(infix: String): String? {
      if (FallbackPropertyConfig.getConfig("webhook.${infix}.destination") != null) {
        return "webhook.${infix}"
      }

      if (FallbackPropertyConfig.getConfig("webhook.default.destination") != null) {
        return "webhook.default"
      }

      return null
    }
  }
}

class FeatureMessagingCloudEventPublisherImpl @Inject constructor(
  private val cloudEventPublisher: CloudEventPublisher, private val messagingConfig: MessagingConfig
) : FeatureMessagingCloudEventPublisher {
  private val log = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)

  override fun publishFeatureMessagingUpdate(
    hooks: List<MappedSupportedConfig>,
    webhookEnvironmentInfo: Map<String, String>,
    converterHandler: () -> FeatureMessagingUpdate
  ) {
    messagingConfig.executor?.let {
      it.submit {
        try {
          val featureMessagingUpdate = converterHandler()

          hooks.forEach { hook ->
            log.trace("publishing feature messaging update for {}", featureMessagingUpdate)
            val event = CloudEventBuilder.v1().newBuilder()
            event.withSubject(FeatureMessagingUpdate.CLOUD_EVENT_SUBJECT)
            // internal type
            event.withContextAttribute("x-ce-type", FeatureMessagingUpdate.CLOUD_EVENT_TYPE)
            event.withId(UUID.randomUUID().toString())
            event.withType(hook.cloudEventType)
            event.withSource(URI("http://mr"))
            event.withTime(OffsetDateTime.now())

            // this is designed to allow us to talk to the destination
            hook.headers.forEach {
              event.withContextAttribute(it.key, it.value)
            }

            // this is all of the config info from the webhook, which can include encrypted data
            featureMessagingUpdate.additionalInfo = hook.additionalProperties(webhookEnvironmentInfo)

            cloudEventPublisher.publish(FeatureMessagingUpdate.CLOUD_EVENT_TYPE, featureMessagingUpdate, event)
          }
        } catch (e: Exception) {
          log.error("Failed to publish messaging update for feature", e)
        }
      }
    }
  }
}
