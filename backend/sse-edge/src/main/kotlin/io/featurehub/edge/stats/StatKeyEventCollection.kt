package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeApiStat
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.featurehub.sse.stats.model.EdgeStatApiKey

/**
 * This represents a single API key and its list of possible stats
 */
class StatKeyEventCollection(val apiKey: KeyParts) {
  private val counters: MutableList<StatCounter> = mutableListOf()

  // this is going to cause us to have duplication in this list but its ok, because the
  // outgoing parser will simply collapse it all. It is also faster and thinner as a solution
  // than a map of maps of atomic integers
  fun add(resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType) {
    var found = counters.find { li -> li.hitSourceType == hitSourceType && li.resultType == resultType }

    if (found == null) {
      found = StatCounter(resultType, hitSourceType)
      counters.add(found)
    }

    found.counter.incrementAndGet()
  }

  fun size(): Int {
    return counters.size;
  }

  fun squash(): EdgeStatApiKey? {
    return EdgeStatApiKey()
      .svcKey(apiKey.serviceKey)
      .envId(apiKey.environmentId)
      .counters(squashCounters())
  }

  private fun squashCounters(): MutableList<EdgeApiStat> {
    val squashed = ArrayList<EdgeApiStat>()

    // if we got duplicates, it squashes their counters together

    counters.forEach { c ->
      var found: EdgeApiStat = squashed.find { s -> s.hitType == c.hitSourceType && s.resultType == c.resultType }
        ?: defaultApiStat(c, squashed)

      val starting = found.count ?: 0L

      found.count(starting + c.counter.get())
    }

    return squashed
  }

  private fun defaultApiStat(c: StatCounter, squashed: java.util.ArrayList<EdgeApiStat>): EdgeApiStat {
    val stat = EdgeApiStat().hitType(c.hitSourceType).resultType(c.resultType)
    squashed.add(stat)
    return stat
  }
}
