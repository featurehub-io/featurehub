package io.featurehub.redis

import io.featurehub.utils.FallbackPropertyConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Gives access to the Redis connection currently configured, or null if there is none. Callers should fetch it once
 * per operation and use that instance throughout, as it can change between calls.
 */
fun interface RedisSource {
  fun current(): FeatureHubRedis?
}

class StaticRedisSource(private val redis: FeatureHubRedis?) : RedisSource {
  override fun current(): FeatureHubRedis? = redis
}

/**
 * A Redis connection whose configuration is re-read periodically from one or more properties files, so Redis can be
 * enabled, changed or disabled on running instances without a restart. In Kubernetes these are typically a mounted
 * ConfigMap and Secret (not using subPath, which does not update), and new instances read the same files at startup.
 *
 * Keys are looked up in the files first (later files win), then in the normal configuration (environment, system
 * properties). A file that does not exist is treated as empty.
 *
 * When the configuration changes, the new connection is swapped in atomically and the old one is closed after a grace
 * period so in-flight requests can finish. If the new configuration is invalid, the current connection is kept.
 */
class ReloadingRedisSource @JvmOverloads constructor(
  private val prefix: String,
  private val files: List<Path>,
  reloadSeconds: Long,
  private val factory: (RedisConfig) -> FeatureHubRedis = { JedisFeatureHubRedis(it) },
  private val closeGraceSeconds: Long = 30,
  private val onChange: (RedisConfig?) -> Unit = {},
  startScheduler: Boolean = true,
) : RedisSource, AutoCloseable {
  private data class Active(val config: RedisConfig, val redis: FeatureHubRedis)

  private val active = AtomicReference<Active?>(null)
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "redis-config-reload").apply { isDaemon = true }
  }

  init {
    reload()
    if (startScheduler && reloadSeconds > 0) {
      scheduler.scheduleWithFixedDelay({ reload() }, reloadSeconds, reloadSeconds, TimeUnit.SECONDS)
    }
    log.info("redis: watching {} for `{}` redis configuration every {}s", files, prefix, reloadSeconds)
  }

  override fun current(): FeatureHubRedis? = active.get()?.redis

  /**
   * Re-reads the configuration, returns true if the active connection changed. Never throws.
   */
  @Synchronized
  fun reload(): Boolean {
    val config = try {
      val props = readFiles()
      RedisConfig.loadFrom(prefix) { props.getProperty(it) ?: FallbackPropertyConfig.getConfig(it) }
    } catch (e: Exception) {
      log.error("redis: invalid `{}` redis configuration, keeping the current connection", prefix, e)
      return false
    }

    val existing = active.get()
    if (config == existing?.config) return false

    val replacement = if (config == null) null else {
      try {
        Active(config, factory(config))
      } catch (e: Exception) {
        log.error("redis: unable to create a connection for {}, keeping the current connection", config, e)
        return false
      }
    }

    active.set(replacement)

    when {
      replacement == null -> log.info("redis: `{}` redis configuration removed, redis is disabled", prefix)
      existing == null -> log.info("redis: `{}` redis is enabled ({})", prefix, replacement.config)
      else -> log.info("redis: `{}` redis configuration changed ({})", prefix, replacement.config)
    }

    existing?.let { old -> closeLater(old.redis) }

    try {
      onChange(replacement?.config)
    } catch (e: Exception) {
      log.warn("redis: change listener failed", e)
    }

    return true
  }

  private fun readFiles(): Properties {
    val props = Properties()
    files.forEach { file ->
      if (Files.isReadable(file)) {
        Files.newBufferedReader(file).use { props.load(it) }
      } else {
        log.trace("redis: config file {} not present", file)
      }
    }
    return props
  }

  private fun closeLater(redis: FeatureHubRedis) {
    val close = Runnable {
      try {
        redis.close()
      } catch (e: Exception) {
        log.debug("redis: failed closing old connection", e)
      }
    }

    if (closeGraceSeconds <= 0) {
      close.run()
    } else {
      try {
        scheduler.schedule(close, closeGraceSeconds, TimeUnit.SECONDS)
      } catch (e: Exception) {
        close.run() // scheduler is shut down
      }
    }
  }

  override fun close() {
    scheduler.shutdownNow()
    active.getAndSet(null)?.redis?.close()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(ReloadingRedisSource::class.java)

    /**
     * Parses a comma separated list of file paths.
     */
    @JvmStatic
    fun files(list: String?): List<Path> =
      list?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { Path.of(it) } ?: listOf()
  }
}
