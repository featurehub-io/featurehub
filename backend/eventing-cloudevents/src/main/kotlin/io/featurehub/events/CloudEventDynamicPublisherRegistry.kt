package io.featurehub.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.encryption.WebhookEncryptionService
import io.featurehub.events.messaging.AdditionalInfoMessage
import io.featurehub.metrics.MetricsCollector
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class CloudEventDynamicDeliveryDetails(
  var url: String?,
  var headers: Map<String, String>?,
  val config: Map<String, Any>,
  val compressed: Boolean
) {
  fun isValid(): Boolean {
    return url?.isNotEmpty() == true
  }

  fun param(key: String): String? {
    return config[key]?.toString()
  }

  fun param(key: String, defaultVal: String): String {
    return config[key]?.toString() ?: defaultVal
  }
}

interface CloudEventDynamicPublisherRegistry {
  /**
   * This is used by something that wants to publish an event. It passes in the list of prefixes that it supports to create
   * publishers for and when the thing wants to configure it, it will pass the destination which the implementation will discover
   * a publisher for and it will then call it back to allow it to create its publisher.
   *
   * This is called by the PROVIDER of the networking infrastructure.
   */
  fun registerDynamicPublisherProvider(
    prefixes: List<String>, callback: (
      config: CloudEventDynamicDeliveryDetails,
      ce: CloudEvent, destination: String,
      destSuffix: String,
      metric: CloudEventChannelMetric
    ) -> Unit
  );

  val supportedPrefixes: List<String>

  /**
   * This is called by the code requiring a publisher.
   */
  fun requireDynamicPublisher(
    destination: String,
    config: CloudEventDynamicDeliveryDetails,
    cloudEventType: String
  ): Boolean

  /**
   * Depending on which platform we are using for messaging, this will get set. It could be null if none is being used
   * (e.g. the REST only platform)
   */
  fun setDefaultPublisherProvider(prefix: String)

  fun confirmDynamicPublisherExists(destination: String): Boolean
  fun publish(
    cloudEventType: String,
    data: AdditionalInfoMessage<*>,
    delivery: CloudEventDynamicDeliveryDetails,
    event: CloudEventBuilder
  )
}

class CloudEventDynamicPublisherRegistryImpl @Inject constructor(
  private val webhookEncryptionService: WebhookEncryptionService,
  private val cloudEventPublisherRegistry: CloudEventPublisherRegistry) :
  CloudEventDynamicPublisherRegistry {
  private val log: Logger = LoggerFactory.getLogger(CloudEventDynamicPublisherRegistryImpl::class.java)
  private val dynamicDelivery: MutableMap<String, (
    config: CloudEventDynamicDeliveryDetails, ce: CloudEvent,
    destination: String, destSuffix: String, metric: CloudEventChannelMetric
  ) -> Unit> =
    mutableMapOf()
  private var defaultPublisher: String? = null
  private var counter = 1
  private val mapper = ObjectMapper().apply { registerModule(KotlinModule.Builder().build()) }

  override fun registerDynamicPublisherProvider(
    prefixes: List<String>,
    callback: (config: CloudEventDynamicDeliveryDetails, ce: CloudEvent, destination: String, destSuffix: String, metric: CloudEventChannelMetric) -> Unit
  ) {
    prefixes.forEach {
      dynamicDelivery[it] = callback
    }
  }

  override val supportedPrefixes: List<String>
    get() = dynamicDelivery.keys.toList()

  private fun extractDestinationType(destination: String): String? {
    val pos = destination.indexOf("//")

    if (pos > 0) {
      return destination.substring(0, pos + 2)
    }

    return null
  }

  override fun requireDynamicPublisher(
    destination: String,
    config: CloudEventDynamicDeliveryDetails,
    cloudEventType: String
  ): Boolean {
    val type = extractDestinationType(destination)
    val pos = destination.indexOf("//")

//    if (type != null) {
//      dynamicDelivery[type]?.let { publisher ->
//        publisher(config, cloudEventType, destination, destination.substring(pos + 2), makeMetric(config, destination))
//        return true
//      }
//    } else if (defaultPublisher != null && dynamicDelivery.containsKey(defaultPublisher)) {
//      dynamicDelivery[defaultPublisher]?.let { publisher ->
//        publisher(config, cloudEventType, destination, destination.substring(pos + 2), makeMetric(config, destination))
//        return true
//      }
//    } else {
//      log.error("Unable to register destination {}, no publisher found", destination)
//    }

    return false
  }

  private fun makeMetric(config: CloudEventDynamicDeliveryDetails, destination: String): CloudEventChannelMetric {
    val dynamicSuffix = (counter++)

    return CloudEventChannelMetric(
      MetricsCollector.counter(
        config.param("metric.fail.name", "dynamic_counter${dynamicSuffix}"),
        config.param("metric.fail.desc", "Failures when trying to publish to ${destination}")
      ),
      MetricsCollector.histogram(
        config.param("metric.histogram.name", "dynamic_histogram${dynamicSuffix}"),
        config.param("metric.histogram.desc", "Updates to ${destination}")
      )
    )
  }

  override fun setDefaultPublisherProvider(prefix: String) {
    defaultPublisher = prefix
  }

  override fun confirmDynamicPublisherExists(destination: String): Boolean {
    val type = extractDestinationType(destination)

    return (type != null && dynamicDelivery.containsKey(type)) || defaultPublisher != null
  }

  override fun publish(
    cloudEventType: String,
    data: AdditionalInfoMessage<*>,
    delivery: CloudEventDynamicDeliveryDetails,
    event: CloudEventBuilder
  ) {
    // if the delivery config is valid, use it, otherwise try and publish it on the bus in case there
    // is a default listener for that message type (e.g. slack)
    if (delivery.isValid()) {
      val destination = delivery.url!!

      val type = extractDestinationType(destination)
      val pos = destination.indexOf("//")

      if (type != null) {
        dynamicDelivery[type]?.let { publish ->
          delivery.headers?.let {
            delivery.headers = webhookEncryptionService.decrypt(it)
          }

          publish(
            delivery,
            event
              .withType(cloudEventType)
              .withDataContentType("application/json")
              .withData(mapper.writeValueAsBytes(data)).build(),
            destination, destination.substring(pos + 2), makeMetric(delivery, destination)
          )
        }
      }

    } else if (cloudEventPublisherRegistry.hasListeners(cloudEventType)) {
      cloudEventPublisherRegistry.publish(cloudEventType, data, event)
    } else {
      log.error("Configuration error - there is no listener for message type {} - dropping", cloudEventType)
    }
  }
}
