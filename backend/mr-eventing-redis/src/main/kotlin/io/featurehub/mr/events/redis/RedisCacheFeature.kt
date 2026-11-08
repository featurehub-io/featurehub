package io.featurehub.mr.events.redis

import io.featurehub.lifecycle.ExecutorPoolDrainageSource
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.redis.JedisFeatureHubRedis
import io.featurehub.redis.RedisConfig
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
    val config = RedisConfig.load() ?: return false

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

    return true
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(RedisCacheFeature::class.java)
  }
}
