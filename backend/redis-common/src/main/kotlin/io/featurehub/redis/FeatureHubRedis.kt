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

class JedisFeatureHubRedis(private val config: RedisConfig) : FeatureHubRedis {
  private val client: UnifiedJedis = createClient(config)
  override val keys: RedisKeys = config.keys

  init {
    log.info("redis: connected using {}", config)
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
    val params = ScanParams().match(pattern).count(batchSize)
    var cursor = ScanParams.SCAN_POINTER_START
    do {
      val result = client.scan(cursor, params)
      if (result.result.isNotEmpty()) {
        consumer(result.result)
      }
      cursor = result.cursor
    } while (cursor != ScanParams.SCAN_POINTER_START)
  }

  override fun close() {
    client.close()
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

    private fun createClient(config: RedisConfig): UnifiedJedis {
      val hosts = config.hosts.map { HostAndPort(it.host, it.port) }

      return when (config.mode) {
        RedisMode.STANDALONE -> JedisPooled(hosts.first(), clientConfig(config))
        RedisMode.CLUSTER -> JedisCluster(hosts.toSet(), clientConfig(config, false))
        RedisMode.SENTINEL -> JedisSentineled(
          config.sentinelMaster!!, clientConfig(config), hosts.toSet(),
          // sentinels typically do not share the data node credentials or database
          DefaultJedisClientConfig.builder()
            .connectionTimeoutMillis(config.connectTimeoutMs)
            .socketTimeoutMillis(config.socketTimeoutMs)
            .ssl(config.tls)
            .build()
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
