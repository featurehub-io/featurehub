package io.featurehub.mr.maintenance

import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.common.DummyPublisher
import io.featurehub.mr.events.redis.RedisFullSync
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The one place that knows how to "publish everything" for a maintenance window. Both the open source
 * `/maintenance/publish-all` endpoint (MR's monitoring port) and the SaaS diagnostic publish-all endpoint delegate
 * to it, so the sequencing below lives in one place.
 */
interface MaintenanceCachePublisher {
  val cachePublishingEnabled: Boolean
  val redisConfigured: Boolean

  /**
   * Starts the publish and returns straight away. With Redis, a full Redis sync runs first and the streaming caches
   * are republished after it, whatever its outcome; the full outcome is logged by RedisFullSync, prefixed with
   * [reason] (e.g. "maintenance-publish-all: finished").
   */
  fun publishAllInBackground(reason: String)
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
  override fun publishAllInBackground(reason: String) {
    val sync = redisFullSync

    if (sync == null) {
      // without Redis there is no self-syncing broadcaster, so this is everything
      cacheSource?.publishToStreamingCaches()
      log.info("{}: redis is not configured, streaming caches published", reason)
      return
    }

    sync.syncInBackgroundThen(reason) { cacheSource?.publishToStreamingCaches() }
    log.info("{}: redis full sync and streaming cache publish started in the background", reason)
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(MaintenancePublisher::class.java)
  }
}
