package io.featurehub.events

import cd.connect.cloudevents.CloudEventUtils
import cd.connect.cloudevents.TaggedCloudEvent
import io.cloudevents.CloudEvent
import io.featurehub.jersey.config.CacheJsonMapper
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Creates a central registry for receiving events
 */

interface CloudEventReceiverRegistry {
  fun <T> listen(clazz: Class<T>, handler: (msg: T) -> Unit) where T : TaggedCloudEvent
  fun process(event: CloudEvent)
}

abstract class CloudEventReceiverRegistryImpl : CloudEventReceiverRegistry {
  data class CallbackHolder(val clazz: Class<*>, val handler: (msg: TaggedCloudEvent) -> Unit )
  protected val eventHandlers = mutableMapOf<String, MutableMap<String, CallbackHolder>>()
  protected val ignoredEvent = mutableMapOf<String, String>()
  protected val log: Logger = LoggerFactory.getLogger(CloudEventReceiverRegistry::class.java)

  @Suppress("UNCHECKED_CAST")
  override fun <T> listen(clazz: Class<T>, handler: (msg: T) -> Unit) where T : TaggedCloudEvent {
    val type = CloudEventUtils.type(clazz)
    val subject = CloudEventUtils.subject(clazz)

    eventHandlers.getOrPut(type) { mutableMapOf() }[subject] =  CallbackHolder(clazz) { msg -> handler(msg as T) }

    log.info("cloudevent: receiving {} / {}", subject, type)
  }
}

/**
 * This is used for testing, its included in the core codebase as its tiny
 */
class CloudEventReceiverRegistryMock : CloudEventReceiverRegistryImpl() {
  override fun process(event: CloudEvent) {
    val handler = eventHandlers[event.type]?.get(event.subject!!)

    if (handler != null) {
      CacheJsonMapper.fromEventData(event, handler.clazz)?.let { eventData ->
        handler.handler(eventData as TaggedCloudEvent)
      }
    }
  }

  fun process(obj: TaggedCloudEvent) {
    val handler = eventHandlers[CloudEventUtils.type(obj.javaClass)]?.get(CloudEventUtils.subject(obj.javaClass))
      ?: throw java.lang.RuntimeException("No handler for " + obj.toString())

    handler.handler(obj)
  }
}

class CloudEventReceiverRegistryProcessor @Inject
  constructor(private val openTelemetryReader: CloudEventsTelemetryReader) : CloudEventReceiverRegistryImpl() {

  override fun process(event: CloudEvent) {
    if (event.subject != null && event.type != null) {
      val handler = eventHandlers[event.type]?.get(event.subject!!)
      if (handler == null) {
        event.subject?.let {it ->
          // if we can't handle this message, warn about it once and then stick it in
          // the ignore bucket
          ignoredEvent.putIfAbsent(event.type, it)?.let {
            log.warn("Did not understand incoming event: {} / {}", event.subject, event.type)
          }
        }
      } else {
        openTelemetryReader.receive(event) {
          CacheJsonMapper.fromEventData(event, handler.clazz)?.let { eventData ->
            if (log.isTraceEnabled) {
              log.trace("cloudevent: incoming message on {}/{} : {}", event.type, event.subject, eventData.toString())
            }

            handler.handler(eventData as TaggedCloudEvent)
          } ?: log.error("cloudevent: failed to handle message {} : {}", event.subject, event.type)
        }
      }
    } else {
      log.error("received a cloud event with no type or subject")
    }
  }
}
