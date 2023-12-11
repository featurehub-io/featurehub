package io.featurehub.messaging

import io.featurehub.metrics.MetricsCollector
import io.prometheus.client.Counter
import io.prometheus.client.Histogram


class FeatureMessagingMetrics {
  companion object {
    val messagingPublishFailureCounter = MetricsCollector.counter("mr_messaging_publish_fail", "Feature update messaging publish failures")
    val messagingPublishHistogram = MetricsCollector.histogram("mr_messaging_publish", "Feature update messaging publish")
  }
}
