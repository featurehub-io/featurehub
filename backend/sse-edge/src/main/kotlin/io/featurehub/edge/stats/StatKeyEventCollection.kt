package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeApiStat
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import io.featurehub.sse.stats.model.EdgeStatApiKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * This represents a single API key and its list of possible stats
 */
class StatKeyEventCollection(val apiKey: KeyParts) {
  private val counters = ConcurrentHashMap<StatType, AtomicLong>()

  private class StatType(val resultType: EdgeHitResultType, val hitSourceType: EdgeHitSourceType) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as StatType

      if (resultType != other.resultType) return false
      if (hitSourceType != other.hitSourceType) return false

      return true
    }

    override fun hashCode(): Int {
      var result = resultType.hashCode()
      result = 31 * result + hitSourceType.hashCode()
      return result
    }
  }

  // this is going to cause us to have duplication in this list but its ok, because the
  // outgoing parser will simply collapse it all. It is also faster and thinner as a solution
  // than a map of maps of atomic integers
  fun add(resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType) {
    val statType = StatType(resultType, hitSourceType)

    (counters.computeIfAbsent(statType) { key -> AtomicLong(0L) }).incrementAndGet()
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

  private fun squashCounters(): List<EdgeApiStat> {
    return counters.entries.map { c ->
       EdgeApiStat().hitType(c.key.hitSourceType).resultType(c.key.resultType)
        .count(c.value.toLong())
    }.toList()
  }
}
