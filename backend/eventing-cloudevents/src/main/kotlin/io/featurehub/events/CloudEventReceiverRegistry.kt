package io.featurehub.events

import cd.connect.cloudevents.CloudEventUtils
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.cloudevents.core.v1.CloudEventBuilder
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

/**
 * Creates a central registry for receiving events
 */

interface CloudEventBaseReceiverRegistry {
  fun hasListeners(cloudEventType: String): Boolean

  /**
   * If you know your TaggedCloudEvent class and message type are the same, use this listener
   */
  fun <T> listen(clazz: Class<T>, handler: (msg: T, ce: CloudEvent) -> Unit) where T : TaggedCloudEvent

  /**
   * If you wish to listen to an event of a particular *type* but which must have a known format, use this listener.
   * Normally you don't need the clazz as the data is being passed directly in, but its required to decomplicate the API.
   */
  fun <T> listen(clazz: Class<T>, type: String, subject: String?, handler: (msg: T, ce: CloudEvent) -> Unit) where T : TaggedCloudEvent
  fun process(event: CloudEvent)

  // if this was published internally then we have the raw data and should use it, but we don't want to faff about
  // with OpenTelemetry & so forth as its an internal call
  fun <T : TaggedCloudEvent>process(data: T, event: CloudEvent)
}

interface CloudEventReceiverRegistry : CloudEventBaseReceiverRegistry {
  fun registry(name: String): CloudEventBaseReceiverRegistry
}

data class CallbackHolder<T: TaggedCloudEvent>(val clazz: Class<T>, val handler: (msg: T, ce: CloudEvent) -> Unit)

class CallbackHolderList(val clazz: Class<out TaggedCloudEvent>) {
  private val list = mutableListOf<CallbackHolder<out TaggedCloudEvent>>()

  fun <T : CallbackHolder<out TaggedCloudEvent>> add(value: T) {
    list.add(value)
  }

  fun size(): Int { return list.size }

  @Suppress("UNCHECKED_CAST")
  fun iterable(): Iterable<CallbackHolder<in TaggedCloudEvent>> {
    return list.map { it as CallbackHolder<in TaggedCloudEvent> }
  }
}

abstract class CloudEventReceiverRegistryImpl (private val registryName: String) : CloudEventBaseReceiverRegistry {
  protected val eventHandlers = ConcurrentHashMap<String, MutableMap<String, CallbackHolderList>>()

  protected val ignoredEvent = ConcurrentHashMap<String, String>()
  protected val log: Logger = LoggerFactory.getLogger(CloudEventReceiverRegistry::class.java)

  override fun <T> listen(clazz: Class<T>, handler: (msg: T, ce: CloudEvent) -> Unit) where T : TaggedCloudEvent {
    registerListener(CloudEventUtils.type(clazz), CloudEventUtils.subject(clazz), clazz, handler)
  }

  @Suppress("UNCHECKED_CAST")
  private fun <T: TaggedCloudEvent> registerListener(type: String, subject: String, clazz: Class<T>, handler: (msg: T, ce: CloudEvent) -> Unit) {
    val handlerList = eventHandlers.computeIfAbsent(type) { mutableMapOf() }.computeIfAbsent(subject) { CallbackHolderList(clazz) }
    handlerList.add(CallbackHolder(clazz) { msg: TaggedCloudEvent, ce: CloudEvent ->
      handler(msg as T, ce )
    })

    if (log.isTraceEnabled) {
      log.trace("cloudevent: registry `{}`, receiving type `{}`, subject `{}`", registryName, type, subject)
    }
  }

  override fun <T : TaggedCloudEvent> listen(clazz: Class<T>, type: String, subject: String?, handler: (msg: T, ce: CloudEvent) -> Unit) {
    registerListener(type, subject?: CloudEventUtils.subject(clazz), clazz, handler)
  }

  override fun hasListeners(cloudEventType: String): Boolean {
    return eventHandlers[cloudEventType]?.isNotEmpty() == true
  }
}

/**
 * This is used for testing, its included in the core codebase as its tiny
 */
class CloudEventReceiverRegistryMock : CloudEventReceiverRegistryImpl("mock"), CloudEventReceiverRegistry {
  private val registries = mutableMapOf<String, CloudEventBaseReceiverRegistry>()
  override fun registry(name: String): CloudEventBaseReceiverRegistry {
    return registries.computeIfAbsent(name) { _ -> CloudEventReceiverRegistryMock() }
  }


  override fun process(event: CloudEvent) {
    eventHandlers[event.type]?.get(event.subject!!)?.let { handlers ->
      CacheJsonMapper.fromEventData(event, handlers.clazz)?.let {  eventData ->
        handlers.iterable().forEach { handler ->
          handler.handler(eventData, event)
        }
      }
    }
  }

  override fun <T : TaggedCloudEvent> process(data: T, event: CloudEvent) {
    eventHandlers[event.type]?.get(event.subject!!)?.let { handlers ->
      handlers.iterable().forEach { handler ->
        handler.handler(data, event)
      }
    }
  }

  fun process(obj: TaggedCloudEvent) {
    val type = CloudEventUtils.type(obj.javaClass)
    val subject = CloudEventUtils.subject(obj.javaClass)
    val handlers = eventHandlers[type]?.get(subject)
      ?: throw java.lang.RuntimeException("No handler for $obj")

    handlers.iterable().forEach { handler ->
      handler.handler(obj,
        CloudEventBuilder().newBuilder().withType(subject).withSource(URI.create("/fred")).withId("1").withTime(OffsetDateTime.now()).withSubject(subject).build())
    }
  }
}

open class CloudEventReceiverRegistryInternal(
  private val registryName: String,
  private val openTelemetryReader: CloudEventsTelemetryReader,
  protected val executorService: ExecutorService) : CloudEventReceiverRegistryImpl(registryName) {

  init {
    log.info("initializing the cloud receiver registry")
  }

  private fun findHandlers(event: CloudEvent): CallbackHolderList? {
    if (event.subject == null || event.type == null) {
      log.error("received a cloud event with no type or subject")
      return null
    }

    val handlers = eventHandlers[event.type]?.get(event.subject!!)

    if (log.isTraceEnabled) {
      log.debug("cloudevent: {} / {} has {} handlers", event.type, event.subject, handlers?.size() ?: 0)
    }

    if (handlers == null) {
      event.subject?.let {
        // if we can't handle this message, warn about it once and then stick it in
        // the ignore bucket
        ignoredEvent.putIfAbsent(event.type, it)?.let {
          log.debug("cloudevent: did not understand incoming event: {} / {}", event.subject, event.type)
        }
      }

      return null
    }

    return handlers
  }

  private fun <T> deliverEvent(event: CloudEvent, eventData: T, handlers: CallbackHolderList) where T: TaggedCloudEvent {
    if (log.isTraceEnabled) {
      log.debug("cloudevent: incoming message in registry {} on {}/{} to {} handlers : {}", registryName, event.type, event.id, handlers.size(), eventData.toString())
    }

    handlers.iterable().forEach { handler ->
      executorService.submit {
        handler.handler(eventData, event)
      }
    }
  }

  override fun process(event: CloudEvent) {
    findHandlers(event)?.let { handlers ->
      openTelemetryReader.receive(event) {
        CacheJsonMapper.fromEventData(event, handlers.clazz)?.let { eventData ->
          deliverEvent(event, eventData, handlers)
        } ?: log.error("cloudevent: failed to handle message {} : {}", event.type, event.id)
      }
    }
  }

  override fun <T : TaggedCloudEvent> process(data: T, event: CloudEvent) {
    findHandlers(event)?.let { handlers ->
      deliverEvent(event, data, handlers)
    }
  }
}

class CloudEventReceiverRegistryProcessor @Inject
  constructor(private val openTelemetryReader: CloudEventsTelemetryReader,
              executorSupplier: ExecutorSupplier) : CloudEventReceiverRegistryInternal("common", openTelemetryReader,
              executorSupplier.executorService(Integer.valueOf(FallbackPropertyConfig.getConfig("cloudevents.receiver-pool-size", "20")))), CloudEventReceiverRegistry {

  private val registries = ConcurrentHashMap<String, CloudEventBaseReceiverRegistry>()

  init {
    registries["common"] = this
  }

  override fun registry(name: String): CloudEventBaseReceiverRegistry {
    return registries.computeIfAbsent(name) { CloudEventReceiverRegistryInternal(name, openTelemetryReader, executorService) }
  }
}
