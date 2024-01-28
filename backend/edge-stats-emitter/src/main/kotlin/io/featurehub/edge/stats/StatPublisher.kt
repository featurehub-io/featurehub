package io.featurehub.edge.stats

import io.featurehub.events.CloudEventPublisherRegistry
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.sse.stats.model.EdgeStatsBundle
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

interface StatPublisher {
  fun publish(cacheName: String, bundle: EdgeStatsBundle)
}

class StatPublisherImpl @Inject constructor(private val publisher: CloudEventPublisherRegistry) : StatPublisher {
  private val log: Logger = LoggerFactory.getLogger(StatPublisherImpl::class.java)

  override fun publish(cacheName: String, bundle: EdgeStatsBundle) {

    try {
      bundle.timestamp(OffsetDateTime.now())

      if (log.isTraceEnabled) {
        log.trace("stat: Publishing /{}/ : {}", cacheName, CacheJsonMapper.mapper.writeValueAsString(bundle))
      }

      publisher.publish(bundle)
    } catch (e : Exception) {
      log.error("Failed to publish stat event", e)
    }
  }
}
