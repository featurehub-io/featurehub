package io.featurehub.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import io.prometheus.client.Histogram
import io.prometheus.client.Summary
import java.util.concurrent.ConcurrentHashMap

class MetricsCollector {
  companion object {
    val histograms: MutableMap<String, Histogram> = ConcurrentHashMap()
    val summaries: MutableMap<String, Summary> = ConcurrentHashMap()
    val gauges: MutableMap<String, Gauge> = ConcurrentHashMap()
    val counters: MutableMap<String, Counter> = ConcurrentHashMap()


    fun histogram(key: String, help: String): Histogram {
      return histograms.computeIfAbsent(key) {
        Histogram.build(it, help).register()!!
      }
    }

    fun summary(key: String, help: String): Summary {
      return summaries.computeIfAbsent(key) {
        Summary.build(it, help).register()!!
      }
    }

    fun gauge(key: String, help: String): Gauge {
      return gauges.computeIfAbsent(key) {
        Gauge.build(it, help).register()!!
      }
    }

    fun counter(key: String, help: String): Counter {
      return counters.computeIfAbsent(key) {
        Counter.build(it, help).register()!!
      }
    }
  }
}
