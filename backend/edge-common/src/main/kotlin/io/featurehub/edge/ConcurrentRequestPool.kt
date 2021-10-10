package io.featurehub.edge

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.edge.features.EdgeConcurrentRequestPool
import io.opentelemetry.context.Context
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ConcurrentRequestPool : EdgeConcurrentRequestPool {
  @ConfigKey("listen.pool-size")
  var edgePoolSize : Int? = 10
  val executor: ExecutorService

  init {
      DeclaredConfigResolver.resolve(this)

    executor = Context.taskWrapping(Executors.newFixedThreadPool(edgePoolSize!!))
  }

  override fun execute(task: Runnable) {
    executor.submit(task)
  }
}
