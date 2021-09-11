package io.featurehub.edge.features

interface EdgeConcurrentRequestPool {
  fun execute(task: Runnable)
}
