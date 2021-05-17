package io.featurehub.edge.stats

import io.featurehub.dacha.api.CacheJsonMapper
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.featurehub.sse.stats.model.EdgeStatsBundle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NATSStatPublisher @Inject constructor(private val nats : NATSSource) : StatPublisher {
  private val log: Logger = LoggerFactory.getLogger(NATSStatPublisher::class.java)

  override fun publish(cacheName: String, bundle: EdgeStatsBundle) {
    bundle.timestamp(OffsetDateTime.now())

    if (log.isDebugEnabled) {
      log.debug("Publishing {}", CacheJsonMapper.mapper.writeValueAsString(bundle))
    }

    nats.connection.publish(ChannelNames.edgeStatsChannel(cacheName),
      CacheJsonMapper.mapper.writeValueAsBytes(bundle))
  }
}
