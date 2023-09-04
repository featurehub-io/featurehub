package io.featurehub.jersey

import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.ExecutorUtil
import io.opentelemetry.context.Context
import jakarta.inject.Inject
import org.glassfish.jersey.server.ManagedAsyncExecutor
import org.glassfish.jersey.spi.ThreadPoolExecutorProvider
import java.util.concurrent.*

@ManagedAsyncExecutor
class ManagedAsyncThreadPoolExecutorProvider
  : ThreadPoolExecutorProvider("fh-jersey-managed-async") {
  private val executorSupplier: ExecutorSupplier = ExecutorUtil()

  /**
   * This ensures the context is wrapped in OpenTelemetry and the context is passed along
   */
  override fun getExecutorService(): ExecutorService {
    return Context.taskWrapping(super.getExecutorService())
  }

  /**
   * this ensures the MDC is passed along with the request so we don't lose the logging
   */
  override fun createExecutor(
    corePoolSize: Int,
    maximumPoolSize: Int,
    keepAliveTime: Long,
    workQueue: BlockingQueue<Runnable>?,
    threadFactory: ThreadFactory?,
    handler: RejectedExecutionHandler?
  ): ThreadPoolExecutor {
    return executorSupplier.executorService(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
      workQueue, threadFactory, handler)
  }
}
