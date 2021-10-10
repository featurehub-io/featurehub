package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts
import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType

interface StatRecorder {
  fun recordHit(apiKey: KeyParts, resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType)
}

class QuietStatRecorder : StatRecorder {
  override fun recordHit(apiKey: KeyParts, resultType: EdgeHitResultType, hitSourceType: EdgeHitSourceType) {
    // ignore data
  }
}
