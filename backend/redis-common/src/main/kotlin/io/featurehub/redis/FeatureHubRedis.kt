package io.featurehub.redis

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisClientConfig
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.JedisSentineled
import redis.clients.jedis.UnifiedJedis
import redis.clients.jedis.exceptions.JedisConnectionException
import redis.clients.jedis.exceptions.JedisException
import redis.clients.jedis.params.ScanParams
import redis.clients.jedis.params.SetParams
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

/**
 * The tiny subset of Redis we need. Kept as an interface so tests can stub it and so we are not tied to Jedis
 * in the calling code.
 *
 * Note: there are deliberately no MULTI transactions here - they are poorly supported across Jedis cluster
 * modes. Writers order their writes so that a reader never sees an index pointing at nothing, and readers
 * validate what they find.
 */
interface FeatureHubRedis : AutoCloseable {
  val keys: RedisKeys

  fun get(key: String): String?
  fun set(key: String, value: String)
  fun del(vararg keys: String): Long

  /**
   * SET NX PX - returns true if we obtained it
   */
  fun setIfAbsent(key: String, value: String, ttlMs: Long): Boolean

  /**
   * Atomically deletes the key only if it still holds the expected value (e.g. releasing a lock we own).
   * Returns true if it was deleted.
   */
  fun deleteIfEquals(key: String, expected: String): Boolean

  /**
   * Atomically resets the key's TTL only if it still holds the expected value (e.g. renewing a lock we own).
   * Returns true if it was renewed.
   */
  fun expireIfEquals(key: String, expected: String, ttlMs: Long): Boolean

  /**
   * Iterates all keys matching the pattern in batches. The pattern must include the hash tag prefix
   * so that cluster mode routes it correctly.
   */
  fun scanKeys(pattern: String, batchSize: Int = 500, consumer: (List<String>) -> Unit)
}

/**
 * Redis is an optional fallback, so it must never stop an application starting. Cluster and sentinel clients connect
 * (slot discovery / master lookup) as soon as they are constructed, so the client is created lazily on first use and
 * re-attempted - at most once per [reconnectBackoffMs] - if Redis is unreachable. Until then every operation fails with
 * a [JedisException], exactly as it would if Redis went away later, which callers already handle.
 *
 * The client configuration (credentials, TLS, CA certificate) is still built eagerly so that a misconfiguration is
 * reported at startup.
 */
class JedisFeatureHubRedis @JvmOverloads constructor(
  private val config: RedisConfig,
  private val reconnectBackoffMs: Long = 5000,
  private val clientFactory: (RedisConfig) -> UnifiedJedis = { createClient(it) },
) : FeatureHubRedis {
  override val keys: RedisKeys = config.keys

  private val lock = Any()
  @Volatile private var connected: UnifiedJedis? = null
  @Volatile private var closed = false
  private var lastFailure = 0L

  init {
    // fail fast on a broken configuration (e.g. an unreadable CA file), without connecting
    clientConfig(config)
    if (config.mode == RedisMode.SENTINEL) sentinelClientConfig(config)
    log.info("redis: configured using {} (connecting on first use)", config)
  }

  private val client: UnifiedJedis
    get() {
      connected?.let { return it }

      synchronized(lock) {
        connected?.let { return it }
        if (closed) throw JedisConnectionException("redis: connection has been closed")

        val now = System.currentTimeMillis()
        if (lastFailure != 0L && now - lastFailure < reconnectBackoffMs) {
          throw JedisConnectionException("redis: not connected, will retry shortly")
        }

        try {
          val c = clientFactory(config)
          connected = c
          lastFailure = 0L
          log.info("redis: connected using {}", config)
          return c
        } catch (e: Exception) {
          lastFailure = System.currentTimeMillis()
          log.warn("redis: unable to connect using {}: {}", config, e.message)
          throw e as? JedisException ?: JedisConnectionException("redis: unable to connect", e)
        }
      }
    }

  override fun get(key: String): String? = client.get(key)

  override fun set(key: String, value: String) {
    client.set(key, value)
  }

  override fun del(vararg keys: String): Long {
    if (keys.isEmpty()) return 0
    // in cluster mode a multi-key DEL is only allowed within one slot - our hash tag guarantees that
    return client.del(*keys)
  }

  override fun setIfAbsent(key: String, value: String, ttlMs: Long): Boolean {
    return "OK" == client.set(key, value, SetParams().nx().px(ttlMs))
  }

  override fun deleteIfEquals(key: String, expected: String): Boolean {
    return 1L == client.eval(DELETE_IF_EQUALS, listOf(key), listOf(expected))
  }

  override fun expireIfEquals(key: String, expected: String, ttlMs: Long): Boolean {
    return 1L == client.eval(EXPIRE_IF_EQUALS, listOf(key), listOf(expected, ttlMs.toString()))
  }

  override fun scanKeys(pattern: String, batchSize: Int, consumer: (List<String>) -> Unit) {
    val c = client
    val params = ScanParams().match(pattern).count(batchSize)
    var cursor = ScanParams.SCAN_POINTER_START
    do {
      val result = c.scan(cursor, params)
      if (result.result.isNotEmpty()) {
        consumer(result.result)
      }
      cursor = result.cursor
    } while (cursor != ScanParams.SCAN_POINTER_START)
  }

  override fun close() {
    val c = synchronized(lock) {
      closed = true
      connected.also { connected = null }
    }
    c?.close()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(JedisFeatureHubRedis::class.java)

    private const val DELETE_IF_EQUALS =
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
    private const val EXPIRE_IF_EQUALS =
      "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end"

    fun clientConfig(config: RedisConfig, includeDatabase: Boolean = true): JedisClientConfig {
      val builder = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(config.connectTimeoutMs)
        .socketTimeoutMillis(config.socketTimeoutMs)
        .clientName("featurehub")

      config.username?.let { builder.user(it) }
      config.password?.let { builder.password(it) }

      if (includeDatabase && config.database != 0) {
        builder.database(config.database)
      }

      if (config.tls) {
        builder.ssl(true)
        config.caCertPath?.let { builder.sslSocketFactory(sslSocketFactory(it)) }
      }

      return builder.build()
    }

    /**
     * Sentinels typically do not share the data node credentials or database, but they do sit behind the same TLS
     * (and so the same private CA).
     */
    fun sentinelClientConfig(config: RedisConfig): JedisClientConfig {
      val builder = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(config.connectTimeoutMs)
        .socketTimeoutMillis(config.socketTimeoutMs)

      if (config.tls) {
        builder.ssl(true)
        config.caCertPath?.let { builder.sslSocketFactory(sslSocketFactory(it)) }
      }

      return builder.build()
    }

    fun createClient(config: RedisConfig): UnifiedJedis {
      val hosts = config.hosts.map { HostAndPort(it.host, it.port) }

      return when (config.mode) {
        RedisMode.STANDALONE -> JedisPooled(hosts.first(), clientConfig(config))
        RedisMode.CLUSTER -> JedisCluster(hosts.toSet(), clientConfig(config, false))
        RedisMode.SENTINEL -> JedisSentineled(
          config.sentinelMaster!!, clientConfig(config), hosts.toSet(),
          // sentinels typically do not share the data node credentials or database
          sentinelClientConfig(config)
        )
      }
    }

    /**
     * Builds a socket factory that trusts the PEM encoded CA certificate(s) in the file, e.g. the
     * server CA of a managed Redis service.
     */
    fun sslSocketFactory(caCertPath: String): SSLSocketFactory {
      val cf = CertificateFactory.getInstance("X.509")
      val certs = FileInputStream(caCertPath).use { cf.generateCertificates(it) }
      if (certs.isEmpty()) throw IllegalArgumentException("No certificates found in $caCertPath")

      val ks = KeyStore.getInstance(KeyStore.getDefaultType())
      ks.load(null, null)
      certs.forEachIndexed { idx, cert -> ks.setCertificateEntry("redis-ca-$idx", cert) }

      val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
      tmf.init(ks)

      val ctx = SSLContext.getInstance("TLS")
      ctx.init(null, tmf.trustManagers, null)
      return ctx.socketFactory
    }
  }
}
