package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import com.lmax.disruptor.EventHandler
import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.prometheus.client.Counter
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * the StatEventHandler is responsible for collecting, against an API key, a list of events, ideally at most one of
 * each type, but they can duplicate if we get hit by concurrent requests, the squash mechanism is required to be
 * able to squash them.
 *
 * It has a "panic" mode where if it gets too many incoming API keys, it is able to eject them before they are collected
 * normally by the timer.
 */

open class StatEventHandler @Inject constructor(private val orchestrator: StatsOrchestrator) : EventHandler<Stat>, StatCollector {
  private var stats: MutableMap<KeyParts, StatKeyEventCollection> = Collections.synchronizedMap(WeakHashMap())

  // we default to zero and just let it push data every x period of time
  @ConfigKey("edge.stats.panic-threshold")
  protected var panicThreshold: Long? = 0

  private val panicExecutor: Executor?

  init {
    DeclaredConfigResolver.resolve(this)

    if (panicThreshold!! > 0) {
      panicExecutor = makeExecutor()
    } else {
      panicExecutor = null
    }
  }

  open protected fun makeExecutor(): Executor {
    return Executors.newSingleThreadExecutor()
  }

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

    if (panicExecutor != null && panicThreshold!! <= stats.size) {
      panicExecutor.execute({
        orchestrator.squashAndPublish(ejectData())
      })
    }
  }

  override fun ejectData(): Map<KeyParts, StatKeyEventCollection> {
    val oldStats = stats
    apiKeyCounter.clear()
    stats = Collections.synchronizedMap(WeakHashMap())
    return oldStats
  }
}

