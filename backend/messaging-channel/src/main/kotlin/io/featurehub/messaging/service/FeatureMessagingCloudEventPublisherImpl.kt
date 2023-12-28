package io.featurehub.messaging.service

import io.cloudevents.core.builder.CloudEventBuilder
import io.featurehub.events.CloudEventDynamicPublisherRegistry
import io.featurehub.events.CloudEventPublisher
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.messaging.MessagingConfig
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*

/**
 * The purpose of this type is to associate a cloud event type with configuration provided by the person who is running
 * FeatureHub. It goes and tells the system using the dynamic publishers to register a listener for a cloud event of type X
 * against a publisher of type Y. Extra headers can be provided for some transport mechanisms (currently only http/https)
 */
class MappedSupportedConfig(val cloudEventType: String, private val prefix: String, private val configInfix: String, dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry) {
  // used to determine where to send this
  val destination = FallbackPropertyConfig.getMandatoryConfig("${prefix}.destination")

  // used to automatically add to each outgoing CE message
  val headers =
    FallbackPropertyConfig.getConfig("${prefix}.headers", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.map {
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
  fun additionalProperties(sourceWebhookMap: Map<String,String>): Map<String, String> {
    return sourceWebhookMap
      .filter { it.key.startsWith(configInfix) }
      .filter { it.key != "$prefix.enabled" }
      .map { it.key to it.value }.toMap()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(MappedSupportedConfig::class.java)

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

@LifecyclePriority(priority = 12) // this is after all the dynamic registries have registered (they register at priority 5)
class FeatureMessagingCloudEventInitializer @Inject constructor(publisher: FeatureMessagingCloudEventPublisher,
                                                          dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry) : LifecycleListener {
  init {
    val hooks = mapOf(Pair("integration.slack", "integration/slack-v1")).map {
      val prefix = MappedSupportedConfig.infix(it.key, dynamicPublisherRegistry)
      if (prefix != null) MappedSupportedConfig(it.value, prefix, it.key, dynamicPublisherRegistry) else null
    }.filterNotNull()

    publisher.setHooks(hooks)
  }
}

class FeatureMessagingCloudEventPublisherImpl @Inject constructor(
  private val cloudEventPublisher: CloudEventPublisher, private val messagingConfig: MessagingConfig
) : FeatureMessagingCloudEventPublisher {
  private val log = LoggerFactory.getLogger(FeatureMessagingCloudEventPublisherImpl::class.java)
  private val hooks = mutableListOf<MappedSupportedConfig>();

  override fun setHooks(hooks: List<MappedSupportedConfig>) {
    this.hooks.clear()
    this.hooks.addAll(hooks)
  }

  override fun publishFeatureMessagingUpdate(
    webhookEnvironmentInfo: Map<String, String>,
    converterHandler: () -> FeatureMessagingUpdate
  ) {
    if (hooks.isEmpty()) return // skip as we have no enabled hooks

    messagingConfig.executor?.let {
      it.submit {
        try {
          val featureMessagingUpdate = converterHandler()
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
}
