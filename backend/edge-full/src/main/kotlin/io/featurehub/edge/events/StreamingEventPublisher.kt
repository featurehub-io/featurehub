package io.featurehub.edge.events

import io.featurehub.events.CloudEventChannelMetric
import io.featurehub.metrics.MetricsCollector

class StreamingEventPublisher {
  companion object {
    val channelMetric: CloudEventChannelMetric

    init {
      channelMetric = CloudEventChannelMetric(
        MetricsCollector.counter("feature_update_failure", "Failures when trying to post feature updates"),
        MetricsCollector.histogram("feature_updates", "Publishing feature updates")
      )
    }
  }
}
