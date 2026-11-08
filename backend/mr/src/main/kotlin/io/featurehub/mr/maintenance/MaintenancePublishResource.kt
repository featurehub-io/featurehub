package io.featurehub.mr.maintenance

import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.redis.RedisFullSync
import io.featurehub.mr.events.redis.RedisSyncResult
import jakarta.inject.Inject
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * What the maintenance endpoint needs from the main application context.
 */
interface MaintenanceCachePublisher {
  val cachePublishingEnabled: Boolean
  val redisConfigured: Boolean

  /**
   * Runs a full Redis sync synchronously. Returns null if another sync holds the lock (or Redis is not configured).
   */
  fun syncRedis(): RedisSyncResult?

  fun publishToStreamingCaches()
}

/**
 * Either service can be missing (cache publishing disabled, Redis not configured).
 */
class MaintenancePublisher(private val cacheSource: CacheSource?, private val redisFullSync: RedisFullSync?) :
  MaintenanceCachePublisher {
  override val cachePublishingEnabled: Boolean = cacheSource != null
  override val redisConfigured: Boolean = redisFullSync != null

  override fun syncRedis(): RedisSyncResult? = redisFullSync?.syncAll("maintenance-publish-all")

  override fun publishToStreamingCaches() {
    cacheSource?.publishToStreamingCaches()
  }
}

/**
 * Publish everything to the caches, for use during a maintenance window. It is registered ONLY on the separate
 * monitoring port (`monitor.port`, which must never be exposed publicly), so a Kubernetes admin can call it after
 * the Admin traffic has been routed to the Maintenance Responder and before MR is shut down:
 *
 *   kubectl port-forward <mr pod> 8701:8701
 *   curl -X POST localhost:8701/maintenance/publish-all
 *
 * At that point nothing is changing features through the Admin API, so this is the last, race-free sync of Redis
 * before MR goes away (with several MR instances, incremental Redis updates from different instances can otherwise
 * overwrite each other out of order; this full sync puts Redis back to exactly the database state).
 *
 * - Streaming caches (Dacha2) are republished in the background, as "Publish All to Cache" does.
 * - If Redis is configured, a full Redis sync (write everything, sweep what is no longer in the database) runs and
 *   the call only returns when it is complete, with what it did. If another sync is already running (e.g. the
 *   startup sync) it returns 409 - retry.
 */
@Path("/maintenance")
class MaintenancePublishResource @Inject constructor(private val publisher: MaintenanceCachePublisher) {
  @POST
  @Path("/publish-all")
  @Produces(MediaType.APPLICATION_JSON)
  fun publishAll(): Response {
    if (!publisher.cachePublishingEnabled) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
        .entity(mapOf("error" to "cache publishing is not enabled")).build()
    }

    log.info("maintenance: publish-all requested")

    var redis: Map<String, Any>? = null
    if (publisher.redisConfigured) {
      val result = publisher.syncRedis()
        ?: return Response.status(Response.Status.CONFLICT)
          .entity(mapOf("error" to "a redis full sync is already running, retry shortly")).build()

      redis = mapOf(
        "environments" to result.environments,
        "serviceAccounts" to result.serviceAccounts,
        "orphanedEnvironments" to result.orphanedEnvironments,
        "orphanedServiceAccounts" to result.orphanedServiceAccounts,
        "removedApiKeys" to result.removedApiKeys,
        "durationMs" to result.durationMs,
      )
    }

    // we have just done the Redis sync, so don't ask Redis to do another one
    publisher.publishToStreamingCaches()

    log.info("maintenance: publish-all complete (redis: {})", redis ?: "not configured")

    return Response.ok(mapOf(
      "cachesPublished" to true,
      "redisConfigured" to (redis != null),
      "redis" to redis,
    )).build()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(MaintenancePublishResource::class.java)
  }
}
