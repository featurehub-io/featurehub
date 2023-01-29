package io.featurehub.events

import cd.connect.app.config.ConfigKey
import cd.connect.cloudevents.CloudEventUtils
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.utils.ExecutorSupplier
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService

/**
 * This is a generic publisher, it just takes cloud events messages and routes them to channels that ask for them,
 * like the reverse of the listener
 */
interface CloudEventPublisher {
  fun hasListeners(type: String): Boolean
  /**
   * parts of the code that want to publish call this and this method will route the two
   */
  fun publish(type: String, data: Any, eventBuilder: CloudEventBuilder)

  /**
   * channels that know how to publish an event register here, so the individual NATS or GCP or Kinesis channels
   */
  fun registerForPublishing(type: String, metric: CloudEventChannelMetric, compress: Boolean, handler: (msg: CloudEvent) -> Unit)
}

class CloudEventPublisherRegistry @Inject constructor(
  private val cloudEventsTelemetryWriter: CloudEventsTelemetryWriter,
  executorSupplier: ExecutorSupplier
) : CloudEventPublisher {
  @ConfigKey("cloudevents.publisher.thread-pool")
  val threadPoolSize: Int? = 20

  val threadPool: ExecutorService

  private val log: Logger = LoggerFactory.getLogger(CloudEventPublisherRegistry::class.java)
  data class CallbackHolder(val type: String, val metric: CloudEventChannelMetric, val compress: Boolean, val handler: (msg: CloudEvent) -> Unit)
  protected val eventHandlers = mutableMapOf<String, MutableList<CallbackHolder>>()

  init {
    threadPool = executorSupplier.executorService(threadPoolSize!!)
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
            log.trace("cloudevent publish: {}", event)
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

  override fun registerForPublishing(type: String, metric: CloudEventChannelMetric, compress: Boolean, handler: (msg: CloudEvent) -> Unit) {
    val handlers = eventHandlers.getOrPut(type) { mutableListOf() }
    handlers.add(CallbackHolder(type, metric, compress, handler))
  }
}
