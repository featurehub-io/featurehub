package io.featurehub.logging

import cd.connect.logging.JsonLogEnhancer
import io.featurehub.metrics.MetricsCollector
import io.prometheus.client.Counter

class MetricLogEnhancer : JsonLogEnhancer {
  override fun getMapPriority(): Int {
    // pre-create
    level("info")
    level("error")
    level("warn")
    level("debug")
    level("trace")
    level("none")

    return 100  // position in order doesn't matter
  }

  private fun level(priority: String): Counter {
    return MetricsCollector.counter("loglevel_$priority", "Logging counter at $priority level")
  }

  override fun map(
    context: MutableMap<String, String>,
    log: MutableMap<String, Any>,
    alreadyEncodedJsonObjects: MutableList<String>
  ) {
    if (log["priority"] != null) {
      level(log["priority"].toString().lowercase()).inc()
    }
  }

  override fun failed(
    context: MutableMap<String, String>?,
    log: MutableMap<String, Any>?,
    alreadyEncodedJsonObjects: MutableList<String>?,
    e: Throwable?
  ) {
    // nothing
  }
}
