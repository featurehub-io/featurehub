package io.featurehub.lifecycle

/**
 * This gets registered in the context when a source needs to ensure it fully finishes its work. It should
 * log a message
 */
interface ExecutorPoolDrainageSource {
  fun drain()
}
