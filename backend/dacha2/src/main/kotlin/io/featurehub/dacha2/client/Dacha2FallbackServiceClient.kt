package io.featurehub.dacha2.client

import cd.connect.openapi.support.ApiResponse
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.jersey.config.CacheJsonMapper
import io.featurehub.metrics.MetricsCollector
import io.featurehub.mr.model.Dacha2Environment
import io.featurehub.mr.model.Dacha2ServiceAccount
import io.featurehub.redis.FeatureHubRedis
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.ServiceUnavailableException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Called when MR becomes available again, with the environment ids and api keys that were served from Redis
 * while it was not.
 */
fun interface Dacha2RecoveryListener {
  fun recovered(environmentIds: Set<UUID>, apiKeys: Set<String>)
}

/**
 * Wraps the normal MR client. If MR is unavailable (connection failure or any 5xx, including the maintenance
 * responder's 503) the "circuit" opens and requests are served from Redis until MR responds again.
 *
 * - MR 4xx responses (e.g. 404 Not Found) are authoritative and are never served from Redis.
 * - A Redis miss is a [ServiceUnavailableException], never a NotFoundException, so the caches do not put the id into
 *   their (non-expiring) miss caches.
 * - Dacha2 only ever reads from Redis.
 */
class Dacha2FallbackServiceClient(
  private val primary: Dacha2ServiceClient,
  private val redis: FeatureHubRedis,
  private val retryIntervalMs: Long = FallbackPropertyConfig.getConfig("dacha2.redis.mr-retry-interval-ms", "10000").toLong(),
  private val clock: () -> Long = System::currentTimeMillis
) : Dacha2ServiceClient {
  @Volatile private var open = false
  @Volatile private var nextProbeAt = 0L
  private val probing = AtomicBoolean(false)

  private val environmentsFromRedis = ConcurrentHashMap.newKeySet<UUID>()
  private val apiKeysFromRedis = ConcurrentHashMap.newKeySet<String>()
  private val recoveryListeners = CopyOnWriteArrayList<Dacha2RecoveryListener>()

  private val keys = redis.keys

  val isOpen: Boolean
    get() = open

  fun addRecoveryListener(listener: Dacha2RecoveryListener) {
    recoveryListeners.add(listener)
  }

  override fun getEnvironment(id: UUID, key: String?): Dacha2Environment {
    return call("env", { primary.getEnvironment(id, key) }) { mrFailure ->
      val env = readRedis("env", mrFailure) { redis.get(keys.environment(id)) }
        ?.let { CacheJsonMapper.mapper.readValue(it, PublishEnvironment::class.java) }
        ?: throw miss("env", "environment $id is not in redis")

      environmentsFromRedis.add(id)
      Dacha2Environment().env(env)
    }
  }

  override fun getServiceAccount(id: String, key: String?): Dacha2ServiceAccount {
    return call("sa", { primary.getServiceAccount(id, key) }) { mrFailure ->
      val sa = readRedis("sa", mrFailure) {
        redis.get(keys.serviceAccountKey(id))?.let { saId ->
          redis.get(keys.serviceAccount(UUID.fromString(saId)))
        }
      }?.let { CacheJsonMapper.mapper.readValue(it, CacheServiceAccount::class.java) }
        // never trust a stale index - the key must still belong to the service account
        ?.takeIf { it.apiKeyClientSide == id || it.apiKeyServerSide == id }
        ?: throw miss("sa", "service account key is not in redis")

      apiKeysFromRedis.add(sa.apiKeyClientSide)
      apiKeysFromRedis.add(sa.apiKeyServerSide)
      Dacha2ServiceAccount().serviceAccount(sa)
    }
  }

  private fun <T> call(type: String, fromMr: () -> T, fromRedis: (Exception?) -> T): T {
    var amProbing = false

    if (open) {
      if (clock() < nextProbeAt || !probing.compareAndSet(false, true)) {
        return fromRedis(null)
      }
      amProbing = true
    }

    try {
      val result = fromMr()
      if (open) closeCircuit()
      return result
    } catch (e: Exception) {
      if (!isFallbackTrigger(e)) {
        // MR answered (e.g. 404/403) - it is up and authoritative
        if (open) closeCircuit()
        throw e
      }

      openCircuit(e)
      return fromRedis(e)
    } finally {
      if (amProbing) probing.set(false)
    }
  }

  private fun isFallbackTrigger(e: Exception): Boolean {
    return e is ProcessingException || e is ServerErrorException
  }

  private fun <T> readRedis(type: String, mrFailure: Exception?, read: () -> T?): T? {
    try {
      val value = read()
      if (value != null) counterHits.labels(type).inc()
      return value
    } catch (e: Exception) {
      counterErrors.inc()
      log.error("dacha2: MR unavailable and unable to read from redis", e)
      // Redis is down as well, surface the original problem
      throw mrFailure ?: ServiceUnavailableException("MR and Redis unavailable")
    }
  }

  private fun miss(type: String, message: String): ServiceUnavailableException {
    counterMisses.labels(type).inc()
    log.debug("dacha2: redis fallback miss: {}", message)
    return ServiceUnavailableException(message)
  }

  @Synchronized
  private fun openCircuit(e: Exception) {
    nextProbeAt = clock() + retryIntervalMs

    if (!open) {
      open = true
      gaugeState.set(1.0)

      val retryAfter = (e as? ServerErrorException)?.response?.getHeaderString("retry-after")
      if (retryAfter != null) {
        log.info("dacha2: management repository is unavailable (maintenance until {}), serving cache misses from redis", retryAfter)
      } else {
        log.info("dacha2: management repository is unavailable ({}), serving cache misses from redis", e.message)
      }
    }
  }

  @Synchronized
  private fun closeCircuit() {
    if (!open) return

    open = false
    gaugeState.set(0.0)

    val envs = environmentsFromRedis.toSet()
    val apiKeys = apiKeysFromRedis.toSet()
    environmentsFromRedis.clear()
    apiKeysFromRedis.clear()

    log.info("dacha2: management repository is available again ({} environments and {} api keys were served from redis)",
      envs.size, apiKeys.size)

    recoveryListeners.forEach {
      try {
        it.recovered(envs, apiKeys)
      } catch (e: Exception) {
        log.error("dacha2: recovery listener failed", e)
      }
    }
  }

  override fun getEnvironment(id: UUID, key: String?, extraHeaders: MutableMap<String, String>?): Dacha2Environment =
    primary.getEnvironment(id, key, extraHeaders)

  override fun getEnvironmentWithHttpInfo(id: UUID, key: String?): ApiResponse<Dacha2Environment> =
    primary.getEnvironmentWithHttpInfo(id, key)

  override fun getServiceAccount(id: String, key: String?, extraHeaders: MutableMap<String, String>?): Dacha2ServiceAccount =
    primary.getServiceAccount(id, key, extraHeaders)

  override fun getServiceAccountWithHttpInfo(id: String, key: String?): ApiResponse<Dacha2ServiceAccount> =
    primary.getServiceAccountWithHttpInfo(id, key)

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Dacha2FallbackServiceClient::class.java)

    private val gaugeState = MetricsCollector.gauge("dacha2_mr_fallback_state", "1 if MR is unavailable and cache misses are served from redis")
    private val counterHits = io.prometheus.client.Counter.build("dacha2_redis_hits_total", "Cache misses served from redis")
      .labelNames("type").register()
    private val counterMisses = io.prometheus.client.Counter.build("dacha2_redis_misses_total", "Cache misses not found in redis")
      .labelNames("type").register()
    private val counterErrors = MetricsCollector.counter("dacha2_redis_errors", "Failures reading from redis")
  }
}
