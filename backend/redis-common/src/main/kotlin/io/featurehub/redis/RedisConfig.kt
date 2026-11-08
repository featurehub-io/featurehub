package io.featurehub.redis

import io.featurehub.utils.FallbackPropertyConfig
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

enum class RedisMode { STANDALONE, SENTINEL, CLUSTER }

data class RedisHost(val host: String, val port: Int)

/**
 * Connection configuration for Redis. The URL is of the form
 *
 *   redis[s]://[user[:password]@]host:port[,host:port...][/db]
 *
 * Multiple hosts are only meaningful for sentinel (they are the sentinels) and cluster (seed nodes).
 */
data class RedisConfig(
  val tls: Boolean,
  val hosts: List<RedisHost>,
  val username: String?,
  val password: String?,
  val database: Int,
  val mode: RedisMode,
  val sentinelMaster: String?,
  val keyPrefix: String,
  val connectTimeoutMs: Int,
  val socketTimeoutMs: Int,
  val caCertPath: String?,
) {
  val keys: RedisKeys by lazy { RedisKeys(keyPrefix) }

  override fun toString(): String {
    // never log the password
    return "RedisConfig(tls=$tls, hosts=$hosts, username=$username, database=$database, mode=$mode, " +
      "sentinelMaster=$sentinelMaster, keyPrefix='$keyPrefix')"
  }

  companion object {
    const val DEFAULT_PREFIX = "redis"

    /**
     * Load the configuration. Each key is looked up as `$prefix.<name>` first and then, if prefix is not "redis",
     * falls back to `redis.<name>`. This lets Dacha2 use `dacha2.redis.*` with different credentials to MR
     * while still sharing the base `redis.*` settings if desired.
     *
     * Returns null if no URL is configured - which means the Redis fallback is disabled.
     */
    @JvmStatic @JvmOverloads
    fun load(prefix: String = DEFAULT_PREFIX): RedisConfig? {
      fun cfg(name: String): String? {
        val v = FallbackPropertyConfig.getConfig("$prefix.$name")
        if (v != null || prefix == DEFAULT_PREFIX) return v?.trim()?.ifEmpty { null }
        return FallbackPropertyConfig.getConfig("$DEFAULT_PREFIX.$name")?.trim()?.ifEmpty { null }
      }

      val url = cfg("url") ?: return null

      return parse(
        url,
        mode = cfg("mode"),
        username = cfg("username"),
        password = cfg("password"),
        sentinelMaster = cfg("sentinel.master"),
        keyPrefix = cfg("key-prefix"),
        connectTimeoutMs = cfg("connect-timeout-ms")?.toInt(),
        socketTimeoutMs = cfg("socket-timeout-ms")?.toInt(),
        caCertPath = cfg("tls.ca-cert"),
      )
    }

    @JvmStatic @JvmOverloads
    fun enabled(prefix: String = DEFAULT_PREFIX): Boolean = load(prefix) != null

    @JvmStatic @JvmOverloads
    fun parse(
      url: String,
      mode: String? = null,
      username: String? = null,
      password: String? = null,
      sentinelMaster: String? = null,
      keyPrefix: String? = null,
      connectTimeoutMs: Int? = null,
      socketTimeoutMs: Int? = null,
      caCertPath: String? = null,
    ): RedisConfig {
      val schemeIdx = url.indexOf("://")
      if (schemeIdx < 0) throw IllegalArgumentException("redis url must start with redis:// or rediss://")
      val scheme = url.substring(0, schemeIdx).lowercase()
      val tls = when (scheme) {
        "redis" -> false
        "rediss" -> true
        else -> throw IllegalArgumentException("redis url scheme must be redis or rediss, not `${scheme}`")
      }

      var rest = url.substring(schemeIdx + 3)

      var urlUser: String? = null
      var urlPassword: String? = null
      val at = rest.lastIndexOf('@')
      if (at >= 0) {
        val userInfo = rest.substring(0, at)
        rest = rest.substring(at + 1)
        val colon = userInfo.indexOf(':')
        if (colon >= 0) {
          urlUser = decode(userInfo.substring(0, colon)).ifEmpty { null }
          urlPassword = decode(userInfo.substring(colon + 1)).ifEmpty { null }
        } else {
          // redis://password@host is the common "AUTH string only" form used by several managed Redis services
          urlPassword = decode(userInfo).ifEmpty { null }
        }
      }

      var database = 0
      val slash = rest.indexOf('/')
      if (slash >= 0) {
        val db = rest.substring(slash + 1)
        rest = rest.substring(0, slash)
        if (db.isNotEmpty()) database = db.toInt()
      }

      val hosts = rest.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { hp ->
        // allow URI style parsing for IPv6 etc
        val uri = URI("redis://$hp")
        RedisHost(uri.host ?: throw IllegalArgumentException("invalid redis host `$hp`"), if (uri.port < 0) 6379 else uri.port)
      }

      if (hosts.isEmpty()) throw IllegalArgumentException("redis url has no hosts")

      val redisMode = when (mode?.lowercase()) {
        null, "", "standalone" -> RedisMode.STANDALONE
        "sentinel" -> RedisMode.SENTINEL
        "cluster" -> RedisMode.CLUSTER
        else -> throw IllegalArgumentException("unknown redis mode `$mode`")
      }

      if (redisMode == RedisMode.SENTINEL && sentinelMaster == null) {
        throw IllegalArgumentException("redis sentinel mode requires sentinel.master")
      }

      if (redisMode == RedisMode.STANDALONE && hosts.size > 1) {
        throw IllegalArgumentException("redis standalone mode supports only a single host")
      }

      return RedisConfig(
        tls = tls,
        hosts = hosts,
        username = username ?: urlUser,
        password = password ?: urlPassword,
        database = database,
        mode = redisMode,
        sentinelMaster = sentinelMaster,
        keyPrefix = keyPrefix ?: "featurehub",
        connectTimeoutMs = connectTimeoutMs ?: 2000,
        socketTimeoutMs = socketTimeoutMs ?: 2000,
        caCertPath = caCertPath,
      )
    }

    private fun decode(s: String) = URLDecoder.decode(s, StandardCharsets.UTF_8)
  }
}
