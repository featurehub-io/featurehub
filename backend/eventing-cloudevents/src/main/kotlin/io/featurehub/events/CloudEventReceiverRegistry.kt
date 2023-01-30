package io.featurehub.events

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.cloudevents.CloudEventUtils
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.utils.ExecutorSupplier
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.concurrent.*

/**
 * Creates a central registry for receiving events
 */

interface CloudEventReceiverRegistry {
  fun <T> listen(clazz: Class<T>, handler: (msg: T, ce: CloudEvent) -> Unit) where T : TaggedCloudEvent
  fun process(event: CloudEvent)
}

abstract class CloudEventReceiverRegistryImpl : CloudEventReceiverRegistry {
  data class CallbackHolder(val clazz: Class<*>, val handler: (msg: TaggedCloudEvent, ce: CloudEvent) -> Unit)

  protected val eventHandlers = mutableMapOf<String, MutableMap<String, MutableList<CallbackHolder>>>()
  protected val ignoredEvent = mutableMapOf<String, String>()
  protected val log: Logger = LoggerFactory.getLogger(CloudEventReceiverRegistry::class.java)

  @Suppress("UNCHECKED_CAST")
  override fun <T> listen(clazz: Class<T>, handler: (msg: T, ce: CloudEvent) -> Unit) where T : TaggedCloudEvent {
    val type = CloudEventUtils.type(clazz)
    val subject = CloudEventUtils.subject(clazz)

    val handlerList = eventHandlers.getOrPut(type) { mutableMapOf() }.getOrPut(subject) { mutableListOf() }
    handlerList.add(CallbackHolder(clazz) { msg, ce: CloudEvent -> handler(msg as T, ce ) })

    if (log.isTraceEnabled) {
      log.trace("cloudevent: receiving {} / {}", subject, type)
    }
  }
}

/**
 * This is used for testing, its included in the core codebase as its tiny
 */
class CloudEventReceiverRegistryMock : CloudEventReceiverRegistryImpl() {
  override fun process(event: CloudEvent) {
    val handlers = eventHandlers[event.type]?.get(event.subject!!)

    if (handlers != null) {
      CacheJsonMapper.fromEventData(event, handlers[0].clazz)?.let { eventData ->
        handlers.parallelStream().forEach { handler ->
          handler.handler(eventData as TaggedCloudEvent, event)
        }
      }
    }
  }

  fun process(obj: TaggedCloudEvent) {
    val type = CloudEventUtils.type(obj.javaClass)
    val subject = CloudEventUtils.subject(obj.javaClass)
    val handlers = eventHandlers[type]?.get(subject)
      ?: throw java.lang.RuntimeException("No handler for " + obj.toString())

    handlers.parallelStream().forEach { handler ->
      handler.handler(obj,
        CloudEventBuilder().newBuilder().withType(subject).withSource(URI.create("/fred")).withId("1").withTime(OffsetDateTime.now()).withSubject(subject).build())
    }
  }
}

class CloudEventReceiverRegistryProcessor @Inject
constructor(private val openTelemetryReader: CloudEventsTelemetryReader, executorSupplier: ExecutorSupplier) : CloudEventReceiverRegistryImpl() {
  @ConfigKey("cloudevents.receiver-pool-size")
  var cachePoolSize: Int? = 20
  val executorService: ExecutorService

  init {
    DeclaredConfigResolver.resolve(this)

    executorService = executorSupplier.executorService(cachePoolSize!!)
  }


  override fun process(event: CloudEvent) {
    if (event.subject == null || event.type == null) {
      log.error("received a cloud event with no type or subject")
      return
    }

    if (log.isTraceEnabled) {
      log.debug("cloudevent: {}", event.subject)
    }

    val handlers = eventHandlers[event.type]?.get(event.subject!!)

    if (handlers == null) {
      event.subject?.let { it ->
        // if we can't handle this message, warn about it once and then stick it in
        // the ignore bucket
        ignoredEvent.putIfAbsent(event.type, it)?.let {
          log.debug("cloudevent: did not understand incoming event: {} / {}", event.subject, event.type)
        }
      }

      return
    }

    openTelemetryReader.receive(event) {
      CacheJsonMapper.fromEventData(event, handlers[0].clazz)?.let { eventData ->
        if (log.isTraceEnabled) {
          log.trace("cloudevent: incoming message on {}/{} : {}", event.type, event.subject, eventData.toString())
        }

        handlers.forEach { handler ->
          executorService.submit {
            handler.handler(eventData as TaggedCloudEvent, event)
          }
        }
      } ?: log.error("cloudevent: failed to handle message {} : {}", event.subject, event.type)
    }
  }
}
