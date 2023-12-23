package io.featurehub.lifecycle

import jakarta.inject.Inject
import org.glassfish.hk2.api.ServiceLocator

/**
 * This gets registered in the context when a source needs to ensure it fully finishes its work. It should
 * log a message
 */
interface ExecutorPoolDrainageSource {
  fun drain()
}

@LifecyclePriority(priority = 20)
class DrainExecutorPool @Inject constructor(private val serviceLocator: ServiceLocator) : LifecycleShutdown {
  override fun shutdown() {
    // shut down all the executor pools that are RUNNING (not all that are registered)
    val drainSources = serviceLocator.getAllServiceHandles(ExecutorPoolDrainageSource::class.java)

    for (drainSource in drainSources) {
      if (drainSource.isActive) {
        drainSource.service.drain()
      }
    }
  }
}

