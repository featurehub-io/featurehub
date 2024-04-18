package io.featurehub.events

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class FakeExecutorService : ExecutorService {
  override fun execute(command: Runnable) {
    TODO("Not yet implemented")
  }

  override fun shutdown() {
    TODO("Not yet implemented")
  }

  override fun shutdownNow(): MutableList<Runnable> {
    TODO("Not yet implemented")
  }

  override fun isShutdown(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isTerminated(): Boolean {
    TODO("Not yet implemented")
  }

  override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> submit(task: Callable<T>): Future<T> {
    return CompletableFuture.completedFuture(task.call());
  }

  override fun <T : Any?> submit(task: Runnable, result: T): Future<T> {
    task.run()
    return CompletableFuture.completedFuture(null);
  }

  override fun submit(task: Runnable): Future<*> {
    task.run()
    return CompletableFuture.completedFuture(null);
  }

  override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> invokeAll(
    tasks: MutableCollection<out Callable<T>>,
    timeout: Long,
    unit: TimeUnit
  ): MutableList<Future<T>> {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
    TODO("Not yet implemented")
  }

  override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
    TODO("Not yet implemented")
  }
}
