package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts

interface StatCollector {
  fun ejectData(): Map<KeyParts, StatKeyEventCollection>
}
