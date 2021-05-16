package io.featurehub.edge.stats

import io.featurehub.sse.stats.model.EdgeHitResultType
import io.featurehub.sse.stats.model.EdgeHitSourceType
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class StatCounter(var resultType: EdgeHitResultType, var hitSourceType: EdgeHitSourceType) {
  val counter = AtomicLong(0)
}
