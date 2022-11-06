package io.featurehub.utils

import io.opentelemetry.context.Context
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.*

interface ExecutorSupplier {
  fun executorService(nThreads: Int): ExecutorService
}

class ExecutorUtil : ExecutorSupplier {
  private val log: Logger = LoggerFactory.getLogger(ExecutorUtil::class.java)

  override fun executorService(nThreads: Int): ExecutorService {

    val tpe = object : ThreadPoolExecutor(
      nThreads, nThreads,
      0L, TimeUnit.MILLISECONDS,
      LinkedBlockingQueue()
    ) {
      override fun afterExecute(r: Runnable?, t: Throwable?) {
        var t = t
        super.afterExecute(r, t)

        if (t == null && r is Future<*>) {
          try {
            val future = r
            if (future.isDone) {
              future.get()
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
      }
    }

    return Context.taskWrapping(tpe)
  }
}
