package io.featurehub.mr.events.redis

import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.lifecycle.ExecutorPoolDrainageSource
import io.featurehub.metrics.MetricsCollector
import io.featurehub.mr.events.common.CacheWalker
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps Redis in step with the database for whole environments and service accounts.
 *
 * Every change (environment, service account or a feature in an environment) just schedules a *refresh* of that
 * object: after a short debounce, the current state is re-read from the database and written (or deleted) in Redis.
 *
 * - all work for one id runs on the same single-threaded worker, so the last write for an id is always the most
 *   recent database read (environment versions do not change on feature value changes, so we cannot compare
 *   versions instead).
 * - N feature changes to one environment within the debounce window collapse into one rewrite.
 * - Redis or database failures are logged and counted - they never propagate back into the publishing code.
 *
 * MULTI-TENANT DEPLOYMENTS: the database re-reads deliberately run on these dedicated worker threads, never on the
 * caller's thread. Callers are often inside a tenant's request; the workers never carry a tenant, so their lookups by
 * id see every tenant. Keep it that way - running a refresh inline would make it tenant filtered, and a
 * "not found" would then delete another tenant's Redis entry. See [RedisFullSync] for the full explanation.
 */
open class RedisCacheWriter @Inject constructor(
  private val redis: FeatureHubRedis,
  private val walkerProvider: Provider<CacheWalker>
) : ExecutorPoolDrainageSource {
  private val debounceMs = FallbackPropertyConfig.getConfig("redis.env-republish-debounce-ms", "250").toLong()
  private val workerCount = FallbackPropertyConfig.getConfig("redis.pool-size", "2").toInt().coerceAtLeast(1)
  private val drainSeconds = FallbackPropertyConfig.getConfig("redis.shutdown-drain-seconds", "10").toLong()

  private val workers: List<ScheduledExecutorService> = (0 until workerCount).map { idx ->
    ScheduledThreadPoolExecutor(1) { r -> Thread(r, "redis-cache-writer-$idx").apply { isDaemon = true } }.apply {
      // when we shut down, still run what is pending (we want them in Redis before MR goes away)
      executeExistingDelayedTasksAfterShutdownPolicy = true
    }
  }

  private val pendingEnvironments = ConcurrentHashMap.newKeySet<UUID>()
  private val pendingServiceAccounts = ConcurrentHashMap.newKeySet<UUID>()
  private val queued = AtomicInteger(0)
  @Volatile private var draining = false

  private val keys = redis.keys

  private fun worker(id: UUID) = workers[Math.floorMod(id.hashCode(), workerCount)]

  fun scheduleEnvironment(envId: UUID) {
    if (draining) {
      log.warn("redis: shutting down, unable to schedule environment {} - the next full sync will repair it", envId)
      return
    }

    if (pendingEnvironments.add(envId)) {
      gaugeQueue.set(queued.incrementAndGet().toDouble())
      worker(envId).schedule({
        pendingEnvironments.remove(envId) // remove BEFORE reading, so changes that arrive during the read reschedule
        gaugeQueue.set(queued.decrementAndGet().toDouble())
        refreshEnvironmentNow(envId)
      }, debounceMs, TimeUnit.MILLISECONDS)
    }
  }

  fun scheduleServiceAccount(serviceAccountId: UUID) {
    if (draining) {
      log.warn("redis: shutting down, unable to schedule service account {} - the next full sync will repair it", serviceAccountId)
      return
    }

    if (pendingServiceAccounts.add(serviceAccountId)) {
      gaugeQueue.set(queued.incrementAndGet().toDouble())
      worker(serviceAccountId).schedule({
        pendingServiceAccounts.remove(serviceAccountId)
        gaugeQueue.set(queued.decrementAndGet().toDouble())
        refreshServiceAccountNow(serviceAccountId)
      }, debounceMs, TimeUnit.MILLISECONDS)
    }
  }

  /**
   * Used by the full sync - runs the refresh on the owning worker (so it is ordered with any incremental change)
   * without a debounce, returns a future so the caller can wait.
   */
  fun refreshEnvironmentOnWorker(envId: UUID): Future<*> = worker(envId).submit { refreshEnvironmentNow(envId) }
  fun refreshServiceAccountOnWorker(saId: UUID): Future<*> = worker(saId).submit { refreshServiceAccountNow(saId) }

  /**
   * Must only be called on the worker for this id.
   */
  protected fun refreshEnvironmentNow(envId: UUID) {
    try {
      val env = walkerProvider.get().publishableEnvironment(envId)
      val key = keys.environment(envId)

      if (env == null) {
        redis.del(key)
        log.trace("redis: environment {} no longer publishable, removed", envId)
      } else {
        redis.set(key, CacheJsonMapper.mapper.writeValueAsString(env))
        log.trace("redis: environment {} written", envId)
      }

      counterWrites.labels("env").inc()
    } catch (e: Exception) {
      counterFailures.labels("env").inc()
      log.error("redis: failed to refresh environment {}", envId, e)
    }
  }

  /**
   * Must only be called on the worker for this id.
   *
   * Write order is: the service account, then its api key indexes, then remove stale indexes. A reader
   * always goes sakey -> sa and validates the key is still in the service account, so it never trusts
   * a stale index.
   */
  protected fun refreshServiceAccountNow(saId: UUID) {
    try {
      val saKey = keys.serviceAccount(saId)
      val previous = redis.get(saKey)?.let { CacheJsonMapper.mapper.readValue(it, CacheServiceAccount::class.java) }
      val current = walkerProvider.get().publishableServiceAccount(saId)

      val previousApiKeys = previous?.let { setOfNotNull(it.apiKeyClientSide, it.apiKeyServerSide) } ?: setOf()

      if (current == null) {
        redis.del(saKey)
        removeIndexesIfOwned(previousApiKeys, saId)
        log.trace("redis: service account {} no longer publishable, removed", saId)
      } else {
        val currentApiKeys = setOfNotNull(current.apiKeyClientSide, current.apiKeyServerSide)
        redis.set(saKey, CacheJsonMapper.mapper.writeValueAsString(current))
        currentApiKeys.forEach { redis.set(keys.serviceAccountKey(it), saId.toString()) }
        removeIndexesIfOwned(previousApiKeys - currentApiKeys, saId)
        log.trace("redis: service account {} written", saId)
      }

      counterWrites.labels("sa").inc()
    } catch (e: Exception) {
      counterFailures.labels("sa").inc()
      log.error("redis: failed to refresh service account {}", saId, e)
    }
  }

  private fun removeIndexesIfOwned(apiKeys: Set<String>, saId: UUID) {
    val owned = apiKeys.map { keys.serviceAccountKey(it) }.filter { redis.get(it) == saId.toString() }
    if (owned.isNotEmpty()) {
      redis.del(*owned.toTypedArray())
    }
  }

  override fun drain() {
    draining = true
    log.info("redis: draining {} pending cache writes", queued.get())
    workers.forEach { it.shutdown() }
    val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(drainSeconds)
    workers.forEach {
      val remaining = deadline - System.currentTimeMillis()
      if (remaining > 0) it.awaitTermination(remaining, TimeUnit.MILLISECONDS)
    }
    val left = queued.get()
    if (left > 0) {
      log.warn("redis: {} cache writes were not completed before shutdown, the next full sync will repair them", left)
    } else {
      log.info("redis: all cache writes completed")
    }
    workers.forEach { it.shutdownNow() }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(RedisCacheWriter::class.java)

    private val counterWrites = io.prometheus.client.Counter.build("mr_redis_writes_total", "Redis cache writes by type")
      .labelNames("type").register()
    private val counterFailures = io.prometheus.client.Counter.build("mr_redis_write_failures_total", "Redis cache write failures by type")
      .labelNames("type").register()
    private val gaugeQueue = MetricsCollector.gauge("mr_redis_env_republish_queue", "Pending (debounced) Redis cache writes")
  }
}
