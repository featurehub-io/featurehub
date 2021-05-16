package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.sse.stats.model.EdgeStatsBundle
import io.prometheus.client.Counter
import io.prometheus.client.Histogram
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.util.*
import javax.inject.Inject

/**
 * This combines a timer and a stats squasher that then asks a StatsPublisher to publish the bundles in
 * sizes based on configuration.
 */

class StatCollectionSquasher @Inject constructor(private val statCollector: StatCollector,
                                                 private val publisher: StatPublisher) : StatsSquashAndPublisher {
  private val log: Logger = LoggerFactory.getLogger(StatCollectionSquasher::class.java)

  // what are the maximum number of apis we will publish per "publish" request
  @ConfigKey("edge.stats.max-apis-per-publish")
  var maxApiKeysPerPublish: java.lang.Long = java.lang.Long(300L)

  companion object Prometheus {
    val publishTimeHistogram = Histogram.build("edge_publish_time", "Time taken to publish stats to NATS").create()
    val successfulPublishing = Counter.build("edge_publish_success", "Number of successes for publishing").create()
    val failedPublishing = Counter.build("edge_publish_failure", "Number of failures for publishing").create()
  }

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun squashAndPublish() {
    val stats = statCollector.ejectData()

    if (stats.isEmpty()) {
      log.debug("stats: no work")
      return
    }

    publishTimeHistogram?.time {
      log.debug("stats: {} records to process", stats.size)
      // split by cache
      val perCachePublish = HashMap<String, EdgeStatsBundle>()

      try {
        for (stat in stats.values) {
          val bundle = perCachePublish.computeIfAbsent(stat.apiKey.cacheName
          ) { k -> EdgeStatsBundle() }

          bundle.apiKeys.add(stat.squash())

          if (bundle.apiKeys.size >= maxApiKeysPerPublish.toLong()) {
            publisher.publish("", bundle)
            perCachePublish.remove(stat.apiKey.cacheName)
            successfulPublishing.inc()
          }
        }

        perCachePublish.values.forEach { bundle ->
          if (bundle.apiKeys.isNotEmpty()) {
            publisher.publish("", bundle)
            successfulPublishing.inc()
          }
        }
      } catch (e: Exception) {
        failedPublishing.inc()
      }
    }
  }
}
