package io.featurehub.edge.stats

import io.featurehub.edge.KeyParts

interface StatsOrchestrator {
  fun squashAndPublish(stats: Map<KeyParts, StatKeyEventCollection>): Boolean
}
