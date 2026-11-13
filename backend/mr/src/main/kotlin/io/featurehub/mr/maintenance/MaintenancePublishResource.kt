package io.featurehub.mr.maintenance

import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.DummyPublisher
import io.featurehub.mr.events.redis.RedisFullSync
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
   * Starts the publish and returns straight away. With Redis, a full Redis sync runs first and the streaming caches
   * are republished after it, whatever its outcome; the full outcome is logged.
   */
  fun publishAllInBackground()
}

/**
 * Either service can be missing (cache publishing disabled, Redis not configured).
 */
class MaintenancePublisher(private val cacheSource: CacheSource?, private val redisFullSync: RedisFullSync?) :
  MaintenanceCachePublisher {
  // with cloud event publishing turned off, a DummyPublisher is bound as the CacheSource: nothing would be published
  override val cachePublishingEnabled: Boolean = cacheSource != null && cacheSource !is DummyPublisher
  override val redisConfigured: Boolean = redisFullSync != null

  /**
   * This is called on a Jersey request thread, so the sync is run on RedisFullSync's own untenanted thread (the orphan
   * sweep is only safe when the database id list covers every tenant), and the streaming caches are republished on
   * that thread once it has finished. We have just done (or tried) the Redis sync, so publishToStreamingCaches does
   * not ask Redis for another one.
   */
  override fun publishAllInBackground() {
    val sync = redisFullSync

    if (sync == null) {
      cacheSource?.publishToStreamingCaches()
      log.info("maintenance: publish-all: redis is not configured, streaming caches published")
      return
    }

    sync.syncInBackgroundThen("maintenance-publish-all") { cacheSource?.publishToStreamingCaches() }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(MaintenancePublisher::class.java)
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
 * It returns 202 straight away and does the work in the background, as a full sync of a large database (or one
 * queued behind MR's startup sync) can outlast any HTTP or port-forward timeout:
 * - if Redis is configured, a full Redis sync (write everything, sweep what is no longer in the database) runs first;
 * - the streaming caches (Dacha2) are then republished, whatever the outcome of the sync;
 * - the full outcome is logged by `RedisFullSync` as `maintenance-publish-all: finished` (INFO) when Redis is a
 *   complete copy of the database, or `finished WITH PROBLEMS` (ERROR) when the sync was skipped because another
 *   instance holds the lock, failed, or failed to write some objects. In those cases fix the cause and call it again.
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
    publisher.publishAllInBackground()

    return Response.accepted(mapOf(
      "redisConfigured" to publisher.redisConfigured,
      "message" to if (publisher.redisConfigured)
        "redis full sync and cache publish started in the background, check the MR log for 'maintenance-publish-all: finished'"
      else "cache publish started",
    )).build()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(MaintenancePublishResource::class.java)
  }
}
