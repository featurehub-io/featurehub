package io.featurehub.utils

import io.opentelemetry.context.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.*

interface ExecutorSupplier {
  fun executorService(nThreads: Int): ExecutorService
  fun executorService(nThreads: Int,
                      maximumPoolSize: Int,
                      keepAliveTime: Long,
                      unit: TimeUnit,
                      workQueue: BlockingQueue<Runnable>?,
                      threadFactory: ThreadFactory?,
                      handler: RejectedExecutionHandler?): ThreadPoolExecutor
}

internal class MDCRunnable (private var runnable: Runnable) : Runnable {
  private val copy: Map<String,String> = MDC.getCopyOfContextMap()

  override fun run() {
    MDC.setContextMap(copy)
    runnable.run()
  }
}

internal class MDCCallable<T>(private val callable: Callable<T>): Callable<T> {
  private val copy: Map<String,String> = MDC.getCopyOfContextMap()
  override fun call(): T {
    MDC.setContextMap(copy)
    return callable.call()
  }
}

class ExecutorUtil : ExecutorSupplier {
  private val log: Logger = LoggerFactory.getLogger(ExecutorUtil::class.java)

  override fun executorService(nThreads: Int): ExecutorService {
    return Context.taskWrapping(executorService(nThreads, nThreads, 0L,
      TimeUnit.MILLISECONDS, null, null, null))
  }

  override fun executorService(nThreads: Int,
                               maximumPoolSize: Int,
                               keepAliveTime: Long,
                               unit: TimeUnit,
                               workQueue: BlockingQueue<Runnable>?,
                               threadFactory: ThreadFactory?,
                               handler: RejectedExecutionHandler?): ThreadPoolExecutor {
    val tpe = object : ThreadPoolExecutor(
      nThreads, maximumPoolSize,
      keepAliveTime, unit, workQueue ?: LinkedBlockingQueue()
    ) {
      override fun <T> submit(task: Callable<T>): Future<T> {
        return super.submit(MDCCallable(task))
      }

      override fun <T> submit(task: Runnable, result: T): Future<T> {
        return super.submit(MDCRunnable(task), result)
      }

      override fun submit(task: Runnable): Future<*> {
        return super.submit(MDCRunnable(task))
      }

      @Throws(InterruptedException::class)
      override fun <T> invokeAll(tasks: Collection<Callable<T>>): List<Future<T>?> {
        return super.invokeAll(tasks.map { MDCCallable(it) })
      }

      @Throws(InterruptedException::class)
      override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
      ): MutableList<Future<T>> {
        return super.invokeAll(tasks.map { MDCCallable(it) }, timeout, unit)
      }

      @Throws(InterruptedException::class, ExecutionException::class)
      override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        return super.invokeAny(tasks.map { MDCCallable(it) })
      }

      @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
      override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        return super.invokeAny(tasks.map { MDCCallable(it) }, timeout, unit)
      }

      override fun execute(command: Runnable) {
        super.execute(MDCRunnable(command))
      }

      override fun afterExecute(r: Runnable?, originalThrowable: Throwable?) {
        try {
          var t = originalThrowable

          super.afterExecute(r, t)

          if (t == null && r is Future<*>) {
            try {
              if (r.isDone) {
                r.get()
              }
            } catch (ce: CancellationException) {
              t = ce
            } catch (ee: ExecutionException) {
              t = ee.cause
            } catch (ie: InterruptedException) {
              Thread.currentThread().interrupt()
            }
          }

          t?.let { log.error("Thread execution failed", t) }
        } finally {
          MDC.clear()
        }
      }
    }

    threadFactory?.let {
      tpe.threadFactory = it
    }

    handler?.let {
      tpe.rejectedExecutionHandler = it
    }

    return tpe
  }
}
