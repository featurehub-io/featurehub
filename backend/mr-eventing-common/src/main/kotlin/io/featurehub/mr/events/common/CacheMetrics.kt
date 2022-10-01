package io.featurehub.mr.events.common

import io.featurehub.metrics.MetricsCollector
import io.prometheus.client.Counter
import io.prometheus.client.Histogram

data class CacheMetric(val counter: Counter, val failures: Counter, val perf: Histogram)

class CacheMetrics {
  companion object {
    val environmentCounter = MetricsCollector.counter(
      "mr_publish_environments_bytes", "Bytes published to channel for environment updates"
    )
    val featureCounter = MetricsCollector.counter(
      "mr_publish_features_bytes", "Bytes published to channel for feature updates."
    )
    val serviceAccountCounter = MetricsCollector.counter(
      "mr_publish_service_accounts_bytes", "Bytes published to channel for service account updates."
    )
    val environmentFailureCounter = MetricsCollector.counter(
      "mr_publish_environments_failed", "Failed to publish to channel for environment updates"
    )
    val featureFailureCounter = MetricsCollector.counter(
      "mr_publish_features_failed", "Failed to publish to channel for feature updates."
    )
    val serviceAccountFailureCounter = MetricsCollector.counter(
      "mr_publish_service_accounts_failed", "Failed to publish to channel for service account updates."
    )
    val environmentGram = MetricsCollector.histogram(
      "mr_publish_environments_histogram", "Histogram for publishing environments"
    )
    val featureGram = MetricsCollector.histogram(
      "mr_publish_features_histogram", "Histogram for publishing features"
    )
    val serviceAccountsGram = MetricsCollector.histogram(
      "mr_publish_service_accounts_histogram", "Histogram for publishing service account"
    )

    val services = CacheMetric(serviceAccountCounter, serviceAccountFailureCounter, serviceAccountsGram)
    val features = CacheMetric(featureCounter, featureFailureCounter, featureGram)
    val environments = CacheMetric(environmentCounter, environmentFailureCounter, environmentGram)
  }


}
