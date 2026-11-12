package io.featurehub.mr.events.redis

import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.metrics.MetricsCollector
import io.featurehub.mr.events.common.CacheWalker
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class RedisSyncResult(
  val reason: String,
  val environments: Int,
  val serviceAccounts: Int,
  val orphanedEnvironments: Int,
  val orphanedServiceAccounts: Int,
  val removedApiKeys: Int,
  val durationMs: Long
)

/**
 * Writes every publishable environment and service account to Redis and then sweeps out anything in Redis
 * that is no longer in the database.
 *
 * - all the work is routed through the [RedisCacheWriter] workers, so it is correctly ordered with incremental
 *   changes happening at the same time.
 * - orphans are found from the *database* id set, and then re-checked against the database on the owning worker
 *   before they are deleted, so an environment created during a sync is never swept away.
 * - a Redis lock ensures only one MR replica runs a sync at a time.
 *
 * MULTI-TENANT DEPLOYMENTS - IMPORTANT
 * Redis keys are not tenant scoped, but database queries can be (e.g. Ebean PARTITION tenancy driven by a thread
 * local). The sweep deletes whatever is in Redis but not in the database id list, so the id list MUST cover every
 * tenant. This is guaranteed because:
 * - syncs run on this class's own `redis-full-sync` thread (see [requestSync]), and the re-reads run on the
 *   [RedisCacheWriter] worker threads - neither ever carries a tenant, as long as the tenant is held in a plain
 *   (non-inheritable) ThreadLocal;
 * - orphans are not deleted directly, they are re-checked on an (untenanted) writer thread first, so even a sync
 *   wrongly run on a tenanted thread rewrites other tenants' keys rather than deleting them.
 * Do not call [syncAll] from request threads, and do not make the sync or the writers run inline on the caller's
 * thread. This behaviour is verified against a real PARTITION tenanted database by a multi-tenant integration spec.
 */
open class RedisFullSync @Inject constructor(
  private val redis: FeatureHubRedis,
  private val writer: RedisCacheWriter,
  private val walkerProvider: Provider<CacheWalker>
) {
  // the lock is renewed every third of this while a sync runs, so it only needs to cover a crashed holder
  private val lockTtlMs = FallbackPropertyConfig.getConfig("redis.full-sync-lock-ms", (2 * 60 * 1000).toString()).toLong()
  private val intervalHours = FallbackPropertyConfig.getConfig("redis.full-sync-interval-hours", "24").toLong()
  private val syncOnStartup = FallbackPropertyConfig.getConfig("redis.full-sync-on-startup", "true") == "true"
  private val batchSize = 1000

  private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "redis-full-sync").apply { isDaemon = true }
  }

  private val keys = redis.keys

  fun start() {
    if (syncOnStartup) {
      requestSync("startup")
    }

    if (intervalHours > 0) {
      executor.scheduleWithFixedDelay({ syncAllSafely("interval") }, intervalHours, intervalHours, TimeUnit.HOURS)
    }
  }

  // separate from the sync thread, as syncs can also run on other threads (e.g. tests, or a caller's own thread)
  private val lockRenewer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "redis-full-sync-lock").apply { isDaemon = true }
  }

  fun stop() {
    lockRenewer.shutdownNow()
    executor.shutdownNow()
  }

  /**
   * Requests a sync in the background (e.g. from a cache refresh or publish-all). This is the method callers should
   * use: the sync then runs on the untenanted `redis-full-sync` thread, even if requested from a tenant's request.
   */
  fun requestSync(reason: String) {
    try {
      executor.submit { syncAllSafely(reason) }
    } catch (e: Exception) {
      log.warn("redis: unable to schedule full sync ({})", reason, e)
    }
  }

  /**
   * Runs a full sync on the untenanted `redis-full-sync` thread and returns its result, for callers (e.g. the
   * maintenance endpoint) that need to wait for it but are on a request thread. Behaves like [syncAll]: the future
   * holds null if another replica holds the sync lock, and fails if the sync fails. If a sync is already running on
   * this instance (e.g. the startup sync) this one runs after it.
   */
  open fun syncOnSyncThread(reason: String): Future<RedisSyncResult?> = executor.submit(Callable { syncAll(reason) })

  private fun syncAllSafely(reason: String) {
    try {
      syncAll(reason)
    } catch (e: Exception) {
      counterSyncFailures.inc()
      log.error("redis: full sync ({}) failed", reason, e)
    }
  }

  /**
   * Runs synchronously on the calling thread. Returns null if another replica holds the sync lock.
   *
   * In a multi-tenant deployment the calling thread must not have a tenant set (see the class notes) - use
   * [requestSync] or [syncOnSyncThread] unless you are on a thread you control.
   */
  open fun syncAll(reason: String): RedisSyncResult? {
    val token = UUID.randomUUID().toString()
    if (!redis.setIfAbsent(keys.syncLock, token, lockTtlMs)) {
      log.info("redis: full sync ({}) skipped, another instance is syncing", reason)
      return null
    }

    // keep the lock while we run (a large sync can outlive the TTL). If it is ever lost, another instance may have
    // started a sync, so we stop before the sweep (the only destructive part)
    val lockLost = AtomicBoolean(false)
    val renewMs = (lockTtlMs / 3).coerceAtLeast(1000)
    val renewal = lockRenewer.scheduleWithFixedDelay({
      try {
        if (!redis.expireIfEquals(keys.syncLock, token, lockTtlMs)) {
          if (lockLost.compareAndSet(false, true)) {
            log.warn("redis: full sync ({}) lost its lock", reason)
          }
        }
      } catch (e: Exception) {
        log.warn("redis: unable to renew the full sync lock", e)
      }
    }, renewMs, renewMs, TimeUnit.MILLISECONDS)

    try {
      val start = System.currentTimeMillis()
      log.info("redis: full sync ({}) starting", reason)

      val walker = walkerProvider.get()
      val envIds = walker.publishableEnvironmentIds().toSet()
      val saIds = walker.publishableServiceAccountIds().toSet()

      waitInBatches(envIds) { writer.refreshEnvironmentOnWorker(it) }
      waitInBatches(saIds) { writer.refreshServiceAccountOnWorker(it) }

      if (lockLost.get()) {
        throw IllegalStateException("redis: full sync ($reason) lost its lock, stopping before the sweep")
      }

      // sweep: anything in Redis we did not find in the database gets re-checked (and removed if gone)
      val orphanEnvs = mutableSetOf<UUID>()
      redis.scanKeys(keys.environmentPattern) { batch ->
        batch.mapNotNull { keys.environmentIdFromKey(it) }.filter { !envIds.contains(it) }.forEach { orphanEnvs.add(it) }
      }
      waitInBatches(orphanEnvs) { writer.refreshEnvironmentOnWorker(it) }

      val orphanSas = mutableSetOf<UUID>()
      redis.scanKeys(keys.serviceAccountPattern) { batch ->
        batch.mapNotNull { keys.serviceAccountIdFromKey(it) }.filter { !saIds.contains(it) }.forEach { orphanSas.add(it) }
      }
      waitInBatches(orphanSas) { writer.refreshServiceAccountOnWorker(it) }

      val removedApiKeys = sweepApiKeyIndexes()

      val result = RedisSyncResult(
        reason, envIds.size, saIds.size, orphanEnvs.size, orphanSas.size, removedApiKeys,
        System.currentTimeMillis() - start
      )

      val completedAt = Instant.now()
      redis.set(keys.metaSync, CacheJsonMapper.mapper.writeValueAsString(mapOf(
        "completedAt" to completedAt.toString(),
        "reason" to reason,
        "environments" to result.environments,
        "serviceAccounts" to result.serviceAccounts,
        "orphanedEnvironments" to result.orphanedEnvironments,
        "orphanedServiceAccounts" to result.orphanedServiceAccounts,
        "removedApiKeys" to result.removedApiKeys,
        "durationMs" to result.durationMs,
      )))

      gaugeLastSync.set(completedAt.epochSecond.toDouble())
      gaugeEnvironments.set(result.environments.toDouble())
      gaugeServiceAccounts.set(result.serviceAccounts.toDouble())

      log.info("redis: full sync complete {}", result)
      return result
    } finally {
      renewal.cancel(false)
      try {
        // atomic compare-and-delete, so we never release a lock another instance now holds
        redis.deleteIfEquals(keys.syncLock, token)
      } catch (e: Exception) {
        log.warn("redis: unable to release sync lock, it will expire", e)
      }
    }
  }

  /**
   * Removes api-key indexes that point at a service account that is gone or no longer holds that key.
   *
   * The scan uses a cached copy of each service account only to find *candidates*. Each candidate is then confirmed
   * against a fresh read on the writer worker that owns that service account, and only deleted there: every write for a
   * service account (account first, then its indexes) happens on that same worker, so a key added during the sync
   * (e.g. a client key reset) can never be mistaken for a stale one.
   */
  private fun sweepApiKeyIndexes(): Int {
    val prefix = keys.serviceAccountKey("")
    val serviceAccounts = mutableMapOf<String, CacheServiceAccount?>()
    val candidates = mutableListOf<Pair<String, String>>() // index key, service account id

    redis.scanKeys(keys.serviceAccountKeyPattern) { batch ->
      batch.forEach { indexKey ->
        val apiKey = indexKey.substring(prefix.length)
        val saId = redis.get(indexKey) ?: return@forEach
        val sa = serviceAccounts.getOrPut(saId) { readServiceAccount(saId) }

        if (!holdsKey(sa, apiKey)) {
          candidates.add(indexKey to saId)
        }
      }
    }

    var removed = 0

    candidates.chunked(batchSize).forEach { chunk ->
      chunk.map { (indexKey, saId) -> confirmAndRemove(indexKey, prefix, saId) }.forEach { if (it.get()) removed++ }
    }

    return removed
  }

  private fun confirmAndRemove(indexKey: String, prefix: String, saId: String): Future<Boolean> {
    val id = try {
      UUID.fromString(saId)
    } catch (e: IllegalArgumentException) {
      null
    }

    // an index holding something that is not a service account id is junk - remove it (still compare-and-delete)
    if (id == null) {
      return java.util.concurrent.CompletableFuture.completedFuture(redis.deleteIfEquals(indexKey, saId))
    }

    return writer.onServiceAccountWorker(id) {
      if (holdsKey(readServiceAccount(saId), indexKey.substring(prefix.length))) {
        false
      } else {
        // compare-and-delete, so an index pointed at another owner in the meantime is never removed
        redis.deleteIfEquals(indexKey, saId)
      }
    }
  }

  private fun readServiceAccount(saId: String): CacheServiceAccount? {
    val id = try {
      UUID.fromString(saId)
    } catch (e: IllegalArgumentException) {
      return null
    }

    return redis.get(keys.serviceAccount(id))?.let { CacheJsonMapper.mapper.readValue(it, CacheServiceAccount::class.java) }
  }

  private fun holdsKey(sa: CacheServiceAccount?, apiKey: String): Boolean =
    sa != null && (sa.apiKeyClientSide == apiKey || sa.apiKeyServerSide == apiKey)

  private fun waitInBatches(ids: Collection<UUID>, submit: (UUID) -> Future<*>) {
    ids.chunked(batchSize).forEach { chunk ->
      chunk.map(submit).forEach { it.get() }
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(RedisFullSync::class.java)

    private val gaugeLastSync = MetricsCollector.gauge("mr_redis_last_full_sync_seconds", "Epoch seconds of the last completed Redis full sync")
    private val gaugeEnvironments = MetricsCollector.gauge("mr_redis_environments", "Environments written by the last Redis full sync")
    private val gaugeServiceAccounts = MetricsCollector.gauge("mr_redis_service_accounts", "Service accounts written by the last Redis full sync")
    private val counterSyncFailures = MetricsCollector.counter("mr_redis_full_sync_failures", "Redis full syncs that failed")
  }
}

/**
 * Lifecycle hooks - start the startup/interval syncs once the app has started, stop them on shutdown.
 */
class RedisFullSyncLifecycle @Inject constructor(private val sync: RedisFullSync) : LifecycleStarted, LifecycleShutdown {
  override fun started() {
    sync.start()
  }

  override fun shutdown() {
    sync.stop()
  }
}
