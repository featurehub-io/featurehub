package io.featurehub.events

import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.events.messaging.AdditionalInfoMessage
import io.featurehub.lifecycle.LifecycleListener
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface DynamicCloudEventDestination {
  // this is expected to be fixed
  val cloudEventType: String

  fun enabled(info: Map<String,String>, orgId: UUID): Boolean
  fun publish(cloudEventType: String, orgId: UUID, data: AdditionalInfoMessage<*>, event: CloudEventBuilder)
}

//@LifecyclePriority(priority = 12) // this is after all the dynamic registries have registered (they register at priority 5)
//class DymamicCloudEventDestinationSourceRegistrar @Inject constructor(
//  sources: IterableProvider<DynamicCloudEventDestinationSource>,
//  listeners: IterableProvider<DynamicCloudEventDestinationListener>,
//  dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry,
//) : LifecycleListener {
//  init {
//    val hooks = sources.map { it.get() }.toList()
//    listeners.forEach { it.setHooks(hooks) }
//  }
//}

/**
 * DynamicCloudEventDestinationMappers are used for configuration at the system properties.
 *
 * The purpose of this type is to associate a cloud event type with configuration provided by the person who is running
 * FeatureHub. It goes and tells the system using the dynamic publishers to register a listener for a cloud event of type X
 * against a publisher of type Y. Extra headers can be provided for some transport mechanisms (currently only http/https)
 */
//class DynamicCloudEventDestinationMapper(
//  override val cloudEventType: String, private val prefix: String,
//  override val configInfix: String, dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry,
//) : DynamicCloudEventDestination {
//  // used to determine where to send this
//  override val destination = FallbackPropertyConfig.getMandatoryConfig("${prefix}.destination")
//
//  // used to automatically add to each outgoing CE message
//  override val headers =
//    FallbackPropertyConfig.getConfig("${prefix}.headers", "").split(",").map { it.trim() }.filter { it.isNotEmpty() }
//      .map {
//        if (it.contains("=")) {
//          val splits = it.split("=")
//          splits[0] to splits[1]
//        } else { // allow for secrets
//          it to FallbackPropertyConfig.getConfig("${prefix}.headers.${it}", "")
//        }
//      }.toMap()
//
//  override fun enabled(info: Map<String, String>): Boolean {
//    return info["$prefix.enabled"] == "true"
//  }
//
//  init {
//    // this will create a new publisher based on config for this type
//    dynamicPublisherRegistry.requireDynamicPublisher(destination, headers, cloudEventType)
//
//    log.info("registered new dynamic publisher $configInfix for destination $destination for CE-type of $cloudEventType (headers $headers)")
//  }
//
//  // augumented into the body of the message
//  override fun additionalProperties(sourceWebhookMap: Map<String, String>): Map<String, String> {
//    return sourceWebhookMap
//      .filter { it.key.startsWith(configInfix) }
//      .filter { it.key != "$prefix.enabled" }
//      .map { it.key to it.value }.toMap()
//  }
//
//  companion object {
//    private val log: Logger = LoggerFactory.getLogger(DynamicCloudEventDestinationMapper::class.java)
//
//    fun infix(infix: String, dynamicPublisherRegistry: CloudEventDynamicPublisherRegistry): String? {
//      if (FallbackPropertyConfig.getConfig("webhook.${infix}.enabled", "false") == "true") {
//        FallbackPropertyConfig.getConfig("webhook.${infix}.destination")?.let {
//          if (dynamicPublisherRegistry.confirmDynamicPublisherExists(it)) {
//            return "webhook.${infix}"
//          }
//        }
//
//        FallbackPropertyConfig.getConfig("webhook.default.destination")?.let {
//          if (dynamicPublisherRegistry.confirmDynamicPublisherExists(it)) {
//            return "webhook.default"
//          }
//        }
//      } else {
//        log.trace("Webhook integration  {} not  found","webhook.${infix}.enabled")
//      }
//
//      return null
//    }
//  }
//}
