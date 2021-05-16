package io.featurehub.edge.stats

import com.lmax.disruptor.EventHandler
import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.prometheus.client.Counter
import java.util.*

class StatEventHandler : EventHandler<Stat>, StatCollector {
  private var stats: MutableMap<KeyParts, StatKeyEventCollection> = Collections.synchronizedMap(WeakHashMap())

  companion object Prometheus {
    val apiKeyCounter = Counter.build("edge_stat_api_key_counter", "Keeps track of how many api keys we have waiting in memory").create()
    val resultTypeCounters = EdgeHitResultType.values().map { v -> v to Counter.build(String.format("edge_stat_result_%s",  v.name.lowercase()),
        String.format("How many results of type %s there are hitting this Edge", v.name)).create() }.toMap()
    val hitTypeCounters = EdgeHitSourceType.values().map { v -> v to Counter.build(
      String.format("edge_stat_hitsource_%s", v.name.lowercase()),
      String.format("Where did this traffic come from: %s", v.name)
    ).create() }.toMap()
  }

  override fun onEvent(stat: Stat, sequence: Long, endOfBatch: Boolean) {
    val sc = stats.computeIfAbsent(stat.apiKey) { key ->
      apiKeyCounter.inc()
      StatKeyEventCollection(key)
    }

    sc.add(stat.resultType, stat.hitSourceType)

    resultTypeCounters[stat.resultType]?.inc()
    hitTypeCounters[stat.hitSourceType]?.inc()
  }

  override fun ejectData(): Map<KeyParts, StatKeyEventCollection> {
    val oldStats = stats
    apiKeyCounter.clear()
    stats = Collections.synchronizedMap(WeakHashMap())
    return oldStats
  }
}

