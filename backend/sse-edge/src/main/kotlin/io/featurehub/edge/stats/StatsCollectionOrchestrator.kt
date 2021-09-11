package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeStatsBundle
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This combines a timer and a stats squasher that then asks a StatsPublisher to publish the bundles in
 * sizes based on configuration.
 */

class StatsCollectionOrchestrator @Inject constructor(private val publisher: StatPublisher) : StatsOrchestrator {
  private val log: Logger = LoggerFactory.getLogger(StatsCollectionOrchestrator::class.java)

  // what are the maximum number of apis we will publish per "publish" request
  @ConfigKey("edge.stats.max-apis-per-publish")
  var maxApiKeysPerPublish: Long? = 300

  companion object Prometheus {
    val publishTimeHistogram = Histogram.build("edge_publish_time", "Time taken to publish stats to NATS").register()
    val successfulPublishing = Counter.build("edge_publish_success", "Number of successes for publishing").register()
    val failedPublishing = Counter.build("edge_publish_failure", "Number of failures for publishing").register()
  }

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun squashAndPublish(stats: Map<KeyParts, StatKeyEventCollection>): Boolean {
    if (stats.isEmpty()) {
      log.trace("stats: no work")
      return true
    }

    var returnVal = true;
    publishTimeHistogram?.time {
      log.trace("stats: {} records to process", stats.size)
      // split by cache
      val perCachePublish = HashMap<String, EdgeStatsBundle>()

      try {
        for (stat in stats) {
          val bundle = perCachePublish.computeIfAbsent(stat.key.cacheName
          ) { EdgeStatsBundle() }

          bundle.apiKeys.add(stat.value.squash())

          if (bundle.apiKeys.size >= maxApiKeysPerPublish!!) {
            publisher.publish(stat.key.cacheName, bundle)
            perCachePublish.remove(stat.key.cacheName)
            successfulPublishing.inc()
          }
        }

        perCachePublish.forEach { bundle ->
          if (bundle.value.apiKeys.isNotEmpty()) {
            publisher.publish(bundle.key, bundle.value)
            successfulPublishing.inc()
          }
        }
      } catch (e: Exception) {
        log.error("Failed to publish collection", e)
        failedPublishing.inc()
        returnVal = false
      }
    }

    return returnVal
  }
}
