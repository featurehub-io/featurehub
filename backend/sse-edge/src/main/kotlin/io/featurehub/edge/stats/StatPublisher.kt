package io.featurehub.edge.stats

import io.featurehub.sse.stats.model.EdgeStatsBundle

interface StatPublisher {
  fun publish(cacheName: String, bundle: EdgeStatsBundle)
}
