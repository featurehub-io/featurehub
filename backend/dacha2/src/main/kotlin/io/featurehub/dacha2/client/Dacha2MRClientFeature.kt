package io.featurehub.dacha2.client

import cd.connect.openapi.support.ApiClient
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.dacha2.api.impl.Dacha2ServiceServiceImpl
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.metrics.MetricsCollector
import io.featurehub.redis.JedisFeatureHubRedis
import io.featurehub.redis.RedisSource
import io.featurehub.redis.ReloadingRedisSource
import io.featurehub.redis.StaticRedisSource
import io.featurehub.redis.RedisConfig
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import jakarta.inject.Singleton
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Dacha2MRClientFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    val redisConfig = RedisConfig.load(REDIS_CONFIG_PREFIX)
    // parsed the same way as the factory does, so e.g. " , " is treated as no files rather than an empty file list
    val configFiles = ReloadingRedisSource.files(FallbackPropertyConfig.getConfig(REDIS_CONFIG_FILES))

    context.register(object: AbstractBinder() {
      override fun configure() {
        if (redisConfig == null && configFiles.isEmpty()) {
          bind(Dacha2Client::class.java).to(Dacha2ServiceClient::class.java).`in`(Immediate::class.java)
        } else {
          if (configFiles.isEmpty()) {
            log.info("dacha2: redis fallback for management repository outages is enabled ({})", redisConfig)
          } else {
            log.info("dacha2: redis fallback configuration will be (re)loaded from {}", configFiles)
          }
          bind(Dacha2FallbackResources::class.java).to(Dacha2FallbackResources::class.java).`in`(Singleton::class.java)
          bindFactory(Dacha2FallbackClientFactory::class.java, Singleton::class.java).to(Dacha2ServiceClient::class.java).`in`(Singleton::class.java)
        }
      }
    })

    if (redisConfig != null || configFiles.isNotEmpty()) {
      LifecycleListeners.shutdown(Dacha2FallbackShutdown::class.java, context)
    }

    return true
  }

  companion object {
    const val REDIS_CONFIG_PREFIX = "dacha2.redis"
    // comma separated properties files re-read at runtime, so redis can be enabled without restarting dacha2
    const val REDIS_CONFIG_FILES = "dacha2.redis.config-file"
    const val REDIS_CONFIG_RELOAD_SECONDS = "dacha2.redis.config-reload-seconds"
    private val log: Logger = LoggerFactory.getLogger(Dacha2MRClientFeature::class.java)
  }
}

class Dacha2Client @Inject constructor(client: Client) : Dacha2ServiceServiceImpl(ApiClient(client,
  FallbackPropertyConfig.getMandatoryConfig("management-repository.url")
))

/**
 * Remembers the fallback client (and the MR JAX-RS client it was given) once they have been created, so shutdown can
 * close them without creating them (injecting Dacha2ServiceClient into a shutdown listener would build it - and start
 * its probe thread - just to close it).
 */
class Dacha2FallbackResources {
  private val created = AtomicReference<Pair<Dacha2FallbackServiceClient, Client?>?>(null)

  @JvmOverloads
  fun created(client: Dacha2FallbackServiceClient, mrClient: Client? = null) {
    created.set(client to mrClient)
  }

  fun close() {
    created.getAndSet(null)?.let { (client, mrClient) ->
      client.close()
      try {
        mrClient?.close()
      } catch (e: Exception) {
        log.warn("dacha2: failed to close the management repository client", e)
      }
    }
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Dacha2FallbackResources::class.java)
  }
}

/**
 * Closes the fallback client (its probe thread, the redis config reload thread and the redis connections) and its
 * dedicated MR JAX-RS client when the container shuts down.
 */
class Dacha2FallbackShutdown @Inject constructor(private val resources: Dacha2FallbackResources) : LifecycleShutdown {
  override fun shutdown() {
    resources.close()
  }
}

class Dacha2FallbackClientFactory @Inject constructor(
  private val client: Client,
  private val resources: Dacha2FallbackResources
) : Supplier<Dacha2ServiceClient> {

  override fun get(): Dacha2ServiceClient {
    // MR gets its own client with short timeouts so an MR outage is detected quickly - we do not change the
    // shared client used for other things
    val mrClient = ClientBuilder.newBuilder().withConfig(client.configuration).build()
    mrClient.property(ClientProperties.CONNECT_TIMEOUT,
      FallbackPropertyConfig.getConfig("dacha2.redis.mr-connect-timeout-ms", "2000").toInt())
    mrClient.property(ClientProperties.READ_TIMEOUT,
      FallbackPropertyConfig.getConfig("dacha2.redis.mr-read-timeout-ms", "15000").toInt())

    return Dacha2FallbackServiceClient(Dacha2Client(mrClient), redisSource()).also { resources.created(it, mrClient) }
  }

  private fun redisSource(): RedisSource {
    val files = ReloadingRedisSource.files(FallbackPropertyConfig.getConfig(Dacha2MRClientFeature.REDIS_CONFIG_FILES))

    if (files.isEmpty()) {
      val config = RedisConfig.load(Dacha2MRClientFeature.REDIS_CONFIG_PREFIX)
      gaugeConfigured.set(if (config == null) 0.0 else 1.0)
      // JedisFeatureHubRedis connects lazily, so an unreachable redis does not stop dacha2 starting
      return StaticRedisSource(config?.let { JedisFeatureHubRedis(it) })
    }

    return ReloadingRedisSource(
      Dacha2MRClientFeature.REDIS_CONFIG_PREFIX,
      files,
      FallbackPropertyConfig.getConfig(Dacha2MRClientFeature.REDIS_CONFIG_RELOAD_SECONDS, "30").toLong(),
      onChange = { config -> gaugeConfigured.set(if (config == null) 0.0 else 1.0) }
    )
  }

  companion object {
    private val gaugeConfigured = MetricsCollector.gauge("dacha2_redis_configured", "1 if a redis fallback connection is configured")
  }
}
