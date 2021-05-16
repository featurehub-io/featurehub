package io.featurehub.edge.stats

import io.featurehub.dacha.api.CacheJsonMapper
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import io.featurehub.sse.stats.model.EdgeStatsBundle
import javax.inject.Singleton

@Singleton
class NATSStatPublisher(private val nats : NATSSource) : StatPublisher {
  override fun publish(cacheName: String, bundle: EdgeStatsBundle) {
    nats.connection.publish(ChannelNames.edgeStatsChannel(cacheName),
      CacheJsonMapper.mapper.writeValueAsBytes(bundle))
  }
}
