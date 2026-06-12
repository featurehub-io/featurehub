package io.featurehub.utils

import io.opentelemetry.context.Context
import org.apache.logging.log4j.ThreadContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
  private val copy: Map<String,String> = MDC.getCopyOfContextMap() ?: emptyMap()

  override fun run() {
    MDC.setContextMap(copy)
    runnable.run()
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
      // All submission paths (submit, invokeAll, invokeAny) flow through execute(),
      // so overriding only execute() is sufficient to propagate MDC without double-wrapping.
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
          // MDC clear only clears the map, not the stack
          ThreadContext.clearAll()
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
