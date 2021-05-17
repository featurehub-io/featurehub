package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType

/**
 * This represents a single event going through the disruptor pipeline, its contents get entirely replaced
 */
class Stat {
  var apiKey: KeyParts = KeyParts("", "", "")
  var resultType: EdgeHitResultType = EdgeHitResultType.MISSED
  var hitSourceType: EdgeHitSourceType = EdgeHitSourceType.EVENTSOURCE

  companion object {
    fun create(apiKey: KeyParts, resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType): Stat{
      val s = Stat()
      s.apiKey = apiKey
      s.resultType = resultType
      s.hitSourceType = hitSourceType
      return s
    }
  }
}
