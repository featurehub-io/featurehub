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
      return histograms.computeIfAbsent(rationaliseKey(key)) {
        Histogram.build(it, help).register()!!
      }
    }

    fun summary(key: String, help: String): Summary {
      return summaries.computeIfAbsent(rationaliseKey(key)) {
        Summary.build(it, help).register()!!
      }
    }

    fun gauge(key: String, help: String): Gauge {
      return gauges.computeIfAbsent(rationaliseKey(key)) {
        Gauge.build(it, help).register()!!
      }
    }

    private fun rationaliseKey(key: String): String {
      var k = key.replace("-", "_").replace(".", "_")
      while (k.endsWith("_")) {
        k = k.substring(0, k.length - 1)
      }
      return k
    }

    fun counter(key: String, help: String): Counter {
      return counters.computeIfAbsent(rationaliseKey(key)) {
        Counter.build(it, help).register()!!
      }
    }
  }
}
