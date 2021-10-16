package io.featurehub.edge.stats

import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.featurehub.sse.stats.model.EdgeStatsBundle
import io.prometheus.client.Counter
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.ConcurrentHashMap

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
        CacheJsonMapper.writeAsZipBytes(bundle))

      prometheusPublishSuccessCounter.computeIfAbsent(cacheName) {
        Counter.build(
          String.format("edge_stat_nats_success_%s", cacheName.replace("-", "_")),
          String.format("Edge Stats NATS Success publishing to channel %s", cacheName)
        ).register()
      }.inc()
    } catch (e : Exception) {
      prometheusPublishFailedCounter.computeIfAbsent(cacheName) {
        Counter.build(
          String.format("edge_stat_nats_failed_%s", cacheName.replace("-", "_")),
          String.format("Edge Stats NATS Failed publishing to channel %s", cacheName)
        ).register()
      }.inc()

      log.error("Failed to publish to channel {}", channelName)
    }
  }
}
