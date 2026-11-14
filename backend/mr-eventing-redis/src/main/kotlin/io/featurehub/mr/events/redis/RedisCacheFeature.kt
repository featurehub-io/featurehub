package io.featurehub.mr.events.redis

import io.featurehub.lifecycle.ExecutorPoolDrainageSource
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.redis.JedisFeatureHubRedis
import io.featurehub.redis.RedisConfig
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Enables writing whole environments and service accounts to Redis, so that Dacha2 can serve cache misses from Redis
 * while MR (and its database) are down for maintenance. Does nothing unless `redis.url` is configured.
 *
 * Requires `CacheWalker` to be bound (DbCacheSource) and is only meaningful where DbCacheSource is the CacheSource.
 *
 * Register this exactly once per MR process (in the main Jersey context), otherwise there will be multiple
 * writers and sync schedulers.
 */
class RedisCacheFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    // invalid config logs an error and leaves Redis off: it is optional, so it must never stop MR starting
    val config = RedisConfig.loadOrDisable() ?: return false

    log.info("redis: Redis fallback cache publishing is enabled ({})", config)

    context.register(object : AbstractBinder() {
      override fun configure() {
        bind(JedisFeatureHubRedis(config)).to(FeatureHubRedis::class.java)
        bind(RedisCacheWriter::class.java).to(RedisCacheWriter::class.java)
          .to(ExecutorPoolDrainageSource::class.java).`in`(Singleton::class.java)
        bind(RedisFullSync::class.java).to(RedisFullSync::class.java).`in`(Singleton::class.java)
        bind(RedisCacheBroadcaster::class.java).to(CacheBroadcast::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.wrap(RedisFullSyncLifecycle::class.java, context)
    LifecycleListeners.shutdown(RedisConnectionShutdown::class.java, context)

    return true
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(RedisCacheFeature::class.java)
  }
}

/**
 * Closes the Redis connection (pool, cluster or sentinel connections and their threads) on shutdown. It runs last, so
 * the cache writer has drained (priority 20) and the full sync has stopped (default priority) before the connection
 * goes away.
 */
@LifecyclePriority(priority = Int.MAX_VALUE)
class RedisConnectionShutdown @Inject constructor(private val redis: FeatureHubRedis) : LifecycleShutdown {
  override fun shutdown() {
    try {
      redis.close()
      log.info("redis: connection closed")
    } catch (e: Exception) {
      log.warn("redis: failed to close the connection", e)
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(RedisConnectionShutdown::class.java)
  }
}
