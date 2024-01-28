package io.featurehub.events

import cd.connect.app.config.ConfigKey
import cd.connect.cloudevents.CloudEventSubject
import cd.connect.cloudevents.CloudEventType
import cd.connect.cloudevents.CloudEventUtils
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.rest.Info
import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ExecutorService


/**
 * This is a generic publisher, it just takes cloud events messages and routes them to channels that ask for them,
 * like the reverse of the listener
 */
interface CloudEventPublisherRegistry {
  fun hasListeners(type: String): Boolean
  /**
   * parts of the code that want to publish call this and this method will route the two
   */
  fun publish(type: String, data: Any, eventBuilder: CloudEventBuilder)

  fun <T : TaggedCloudEvent>  publish(data: T, eventEnricher: (ce: CloudEventBuilder) -> CloudEventBuilder = { ce -> ce })

  /**
   * channels that know how to publish an event register here, so the individual NATS or GCP or Kinesis channels
   */
  fun registerForPublishing(type: String, metric: CloudEventChannelMetric, compress: Boolean, handler: (msg: CloudEvent) -> Unit)
}

class CloudEventPublisherRegistryProcessor @Inject constructor(
  private val cloudEventsTelemetryWriter: CloudEventsTelemetryWriter,
  executorSupplier: ExecutorSupplier
) : CloudEventPublisherRegistry {
  @ConfigKey("cloudevents.publisher.thread-pool")
  val threadPoolSize: Int? = 20

  val threadPool: ExecutorService

  private val log: Logger = LoggerFactory.getLogger(CloudEventPublisherRegistryProcessor::class.java)
  data class CallbackHolder(val type: String, val metric: CloudEventChannelMetric, val compress: Boolean, val handler: (msg: CloudEvent) -> Unit)
  protected val eventHandlers = mutableMapOf<String, MutableList<CallbackHolder>>()
  protected val defaultCloudEventSource: String

  init {
    threadPool = executorSupplier.executorService(threadPoolSize!!)
    defaultCloudEventSource = FallbackPropertyConfig.getConfig("cloudevents.outbound.source", "http://${Info.applicationName()}")
  }

  override fun hasListeners(type: String): Boolean {
    return eventHandlers[type] != null
  }

  fun publishEvent(handlers: List<CallbackHolder>, eventBuilder: CloudEventBuilder) {
    // we synchronously cycle through the handlers as we need to attach the current thread's data to it
    handlers.forEach { handler ->
      cloudEventsTelemetryWriter.publish(
        handler.type, eventBuilder,
        CloudEventChannelMetric(handler.metric.failures, handler.metric.perf)
      ) { evt ->
        val event = eventBuilder.build()
        threadPool.submit {
          if (log.isTraceEnabled) {
            log.trace("cloudevent publish: type {}, subject={}, id={}", event.type, event.subject, event.id)
          }
          handler.handler(event)
        }
      }
    }
  }

  override fun publish(type: String, data: Any, eventBuilder: CloudEventBuilder) {
    eventBuilder.withType(type)

    if (data is TaggedCloudEvent) {
      eventBuilder.withSubject(CloudEventUtils.subject(data.javaClass))
    }

    publishFullyFormed(type, data, eventBuilder)
  }

  private fun publishFullyFormed(type: String, data: Any, eventBuilder: CloudEventBuilder) {
    val handlers = eventHandlers[type]

    if (handlers == null) {
      log.error("Attempting to publish event with no destination {} : {}", eventBuilder, data)
    } else {
      val compressHandlers = handlers.filter { it.compress }

      if (compressHandlers.isNotEmpty()) {
        CacheJsonMapper.toEventData(eventBuilder, data, true)
        publishEvent(compressHandlers, eventBuilder)
      }

      val uncompressedHandlers = handlers.filter { !it.compress }

      if (uncompressedHandlers.isNotEmpty()) {
        CacheJsonMapper.toEventData(eventBuilder, data, false)
        publishEvent(uncompressedHandlers, eventBuilder)
      }
    }
  }

  override fun <T : TaggedCloudEvent> publish(data: T, eventEnricher: (ce: CloudEventBuilder) -> CloudEventBuilder) {
    val type = data.javaClass.getAnnotation(CloudEventType::class.java).value
    val subject = data.javaClass.getAnnotation(CloudEventSubject::class.java).value

    val event = io.cloudevents.core.builder.CloudEventBuilder.v1().newBuilder().apply {
      withSubject(subject)
      withId(UUID.randomUUID().toString())
      withType(type)
      withSource(URI(defaultCloudEventSource))
      withTime(OffsetDateTime.now())

      eventEnricher(this)
    }

    publishFullyFormed(type, data, event)
  }

  override fun registerForPublishing(type: String, metric: CloudEventChannelMetric, compress: Boolean, handler: (msg: CloudEvent) -> Unit) {
    val handlers = eventHandlers.getOrPut(type) { mutableListOf() }
    handlers.add(CallbackHolder(type, metric, compress, handler))
  }


}
