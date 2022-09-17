package io.featurehub.db.publish.nats

import com.fasterxml.jackson.core.JsonProcessingException
import io.featurehub.dacha.model.CacheManagementMessage
import io.featurehub.dacha.model.CacheRequestType
import io.featurehub.dacha.model.CacheState
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.listeners.FeatureUpdateListener
import io.featurehub.mr.messaging.StreamedFeatureUpdate
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.nats.client.Dispatcher
import io.nats.client.Message
import io.nats.client.MessageHandler
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * This code is designed to support Dacha (1) - it supports listening to the feature updates channel and
 * processing events through it, and also acting as the master source for Dacha 1's broadcast resolution.
 */
class NatsDachaCacheFiller @Inject constructor(val cacheName: String, val nats: NATSSource,
                                               private val id: UUID, val cacheSource: CacheSource,
                                               private val updateListener: FeatureUpdateListener
) {
  private val managementSubject: String
  private val featureUpdaterSubject: String
  private val managementDispatcher: Dispatcher
  private val featureUpdaterDispatcher: Dispatcher

  private val log: Logger = LoggerFactory.getLogger(NatsDachaCacheFiller::class.java)

  init {
    managementSubject = ChannelNames.managementChannel(cacheName)
    log.debug("listening on management subject `{}`", managementSubject)

    managementDispatcher = nats.connection.createDispatcher(object: MessageHandler {
      override fun onMessage(msg: Message) {
        managementMessage(msg)
      }
    }).subscribe(managementSubject)

    featureUpdaterSubject = "/$cacheName/feature-updates"

    log.info("Listening for feature updates on {}", featureUpdaterSubject)

    // this is a QUEUE, not a pub/sub topic
    featureUpdaterDispatcher = nats.connection.createDispatcher(object: MessageHandler {
      override fun onMessage(msg: Message) {
        streamingFeatureUpdate(msg)
      }

    }).subscribe(featureUpdaterSubject, "feature-updates")
  }

  private fun streamingFeatureUpdate(message: Message) {
    try {
      val update = CacheJsonMapper.readFromZipBytes(message.data, StreamedFeatureUpdate::class.java)

      updateListener.processUpdate(update)
    } catch (e: java.lang.Exception) {
      log.error("Unable to parse incoming request for update", e)
    }
  }

  fun managementMessage(message: Message) {
    try {
      val cmm = CacheJsonMapper.mapper.readValue(message.data, CacheManagementMessage::class.java)
      log.trace("incoming message {}", cmm.toString())

      // ignore messages not directed at us or our own messages
      if (cmm.destId != null && id != cmm.destId || id == cmm.id) {
        return
      }
      if (cmm.requestType == CacheRequestType.SEEKING_COMPLETE_CACHE) {
        sayHelloToNewNamedCache()
      } else if (id == cmm.destId && cmm.requestType == CacheRequestType.SEEKING_REFRESH) {
        cacheSource.publishObjectsAssociatedWithCache(cacheName)
      }
    } catch (e: Exception) {
      log.error("Malformed cache management message", e)
    }
  }

  fun close() {
    managementDispatcher.unsubscribe(managementSubject)
    featureUpdaterDispatcher.unsubscribe(featureUpdaterSubject)
  }

  private fun sayHelloToNewNamedCache() {
    try {
      log.trace("responding with complete cache message to {}", managementSubject)
      nats.connection.publish(
        managementSubject,
        CacheJsonMapper.mapper.writeValueAsBytes(
          CacheManagementMessage()
            .mit(1L)
            .id(id)
            .cacheState(CacheState.COMPLETE)
            .requestType(CacheRequestType.CACHE_SOURCE)
        )
      )
    } catch (e: JsonProcessingException) {
      log.error("Unable to say hello as cannot encode message", e)
    }
  }

}
