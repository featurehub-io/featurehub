package io.featurehub.db.publish.nats

import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.common.CacheMetric
import io.featurehub.mr.events.common.CacheMetrics
import io.featurehub.publish.ChannelNames
import io.featurehub.publish.NATSSource
import jakarta.inject.Inject
import org.slf4j.LoggerFactory

/**  */
class NATSDachaBroadcaster @Inject constructor(private val  nats: NATSSource) : CacheBroadcast {
  private val envChannelNameCache = mutableMapOf<String, String>()
  private val serviceAccountChannelNameCache = mutableMapOf<String, String>()
  private val featureChannelNameCache = mutableMapOf<String, String>()

  override fun publishEnvironment(cacheName: String, eci: PublishEnvironment) {
    publish(
      envChannelNameCache.getOrPut(cacheName) { ChannelNames.environmentChannel(cacheName) },
      eci, "environment", CacheMetrics.environments)

  }

  override fun publishServiceAccount(cacheName: String, saci: PublishServiceAccount) {
    publish(
      serviceAccountChannelNameCache.getOrPut(cacheName) { ChannelNames.serviceAccountChannel(cacheName) },
      saci, "service account", CacheMetrics.services
    )
  }

  override fun publishFeature(cacheName: String, feature: PublishFeatureValue) {
    publish(
      featureChannelNameCache.getOrPut(cacheName) { ChannelNames.featureValueChannel(cacheName) },
      feature, "feature", CacheMetrics.features
    )
  }

  private fun publish(subject: String, obj: Any, desc: String, metrics: CacheMetric) {
    try {
      if (log.isTraceEnabled) {
        log.trace("exporting {} : {}", desc, CacheJsonMapper.mapper.writeValueAsString(obj))
      }

      val body = CacheJsonMapper.writeAsZipBytes(obj)

      metrics.counter.inc(body.size.toDouble())

      val timer = metrics.perf.startTimer()
      try {
        nats.connection.publish(subject, body)
      } finally {
        timer.observeDuration()
      }
    } catch (e: Exception) {
      metrics.failures.inc();

      log.error("failed to write {}", desc, e)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(NATSDachaBroadcaster::class.java)
  }
}
