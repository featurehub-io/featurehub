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
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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
  private val lockTtlMs = FallbackPropertyConfig.getConfig("redis.full-sync-lock-ms", (30 * 60 * 1000).toString()).toLong()
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

  fun stop() {
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
   * [requestSync] unless you are on a thread you control.
   */
  fun syncAll(reason: String): RedisSyncResult? {
    val token = UUID.randomUUID().toString()
    if (!redis.setIfAbsent(keys.syncLock, token, lockTtlMs)) {
      log.info("redis: full sync ({}) skipped, another instance is syncing", reason)
      return null
    }

    try {
      val start = System.currentTimeMillis()
      log.info("redis: full sync ({}) starting", reason)

      val walker = walkerProvider.get()
      val envIds = walker.publishableEnvironmentIds().toSet()
      val saIds = walker.publishableServiceAccountIds().toSet()

      waitInBatches(envIds) { writer.refreshEnvironmentOnWorker(it) }
      waitInBatches(saIds) { writer.refreshServiceAccountOnWorker(it) }

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
      try {
        if (redis.get(keys.syncLock) == token) {
          redis.del(keys.syncLock)
        }
      } catch (e: Exception) {
        log.warn("redis: unable to release sync lock, it will expire", e)
      }
    }
  }

  /**
   * Removes api-key indexes that point at a service account that is gone or no longer holds that key.
   */
  private fun sweepApiKeyIndexes(): Int {
    val prefix = keys.serviceAccountKey("")
    var removed = 0
    val serviceAccounts = mutableMapOf<String, CacheServiceAccount?>()

    redis.scanKeys(keys.serviceAccountKeyPattern) { batch ->
      val stale = batch.filter { indexKey ->
        val apiKey = indexKey.substring(prefix.length)
        val saId = redis.get(indexKey) ?: return@filter false
        val sa = serviceAccounts.getOrPut(saId) {
          redis.get(keys.serviceAccount(UUID.fromString(saId)))?.let {
            CacheJsonMapper.mapper.readValue(it, CacheServiceAccount::class.java)
          }
        }
        sa == null || (sa.apiKeyClientSide != apiKey && sa.apiKeyServerSide != apiKey)
      }

      if (stale.isNotEmpty()) {
        removed += redis.del(*stale.toTypedArray()).toInt()
      }
    }

    return removed
  }

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
