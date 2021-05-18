package io.featurehub.edge.stats

import io.featurehub.dacha.api.CacheJsonMapper
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.featurehub.sse.stats.model.EdgeStatsBundle
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NATSStatPublisher @Inject constructor(private val nats : NATSSource) : StatPublisher {
  private val log: Logger = LoggerFactory.getLogger(NATSStatPublisher::class.java)

  private val prometheusPublishSuccessCounter = ConcurrentHashMap<String, Counter>()
  private val prometheusPublishFailedCounter = ConcurrentHashMap<String, Counter>()

  override fun publish(cacheName: String, bundle: EdgeStatsBundle) {
    val channelName = ChannelNames.edgeStatsChannel(cacheName)

    try {
      bundle.timestamp(OffsetDateTime.now())

      if (log.isTraceEnabled) {
        log.trace("stat: Publishing /{}/ : {}", cacheName, CacheJsonMapper.mapper.writeValueAsString(bundle))
      }

      nats.connection.publish(channelName,
        CacheJsonMapper.mapper.writeValueAsBytes(bundle))

      prometheusPublishSuccessCounter.computeIfAbsent(cacheName) { k ->
        Counter.build(
          String.format("edge_stat_nats_success_%s", cacheName.replace("-", "_")),
          String.format("Edge Stats NATS Success publishing to channel %s", cacheName)
        ).create()
      }.inc()
    } catch (e : Exception) {
      prometheusPublishFailedCounter.computeIfAbsent(cacheName) { k ->
        Counter.build(
          String.format("edge_stat_nats_failed_%s", cacheName.replace("-", "_")),
          String.format("Edge Stats NATS Failed publishing to channel %s", cacheName)
        ).create()
      }.inc()

      log.error("Failed to publish to channel {}", channelName)
    }
  }
}
