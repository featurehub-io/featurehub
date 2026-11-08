package io.featurehub.dacha2.client

import cd.connect.openapi.support.ApiClient
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.dacha2.api.impl.Dacha2ServiceServiceImpl
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
import java.util.function.Supplier
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Dacha2MRClientFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    val redisConfig = RedisConfig.load(REDIS_CONFIG_PREFIX)
    val configFiles = FallbackPropertyConfig.getConfig(REDIS_CONFIG_FILES)

    context.register(object: AbstractBinder() {
      override fun configure() {
        if (redisConfig == null && configFiles.isNullOrBlank()) {
          bind(Dacha2Client::class.java).to(Dacha2ServiceClient::class.java).`in`(Immediate::class.java)
        } else {
          if (configFiles.isNullOrBlank()) {
            log.info("dacha2: redis fallback for management repository outages is enabled ({})", redisConfig)
          } else {
            log.info("dacha2: redis fallback configuration will be (re)loaded from {}", configFiles)
          }
          bindFactory(Dacha2FallbackClientFactory::class.java, Singleton::class.java).to(Dacha2ServiceClient::class.java).`in`(Singleton::class.java)
        }
      }
    })

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

class Dacha2FallbackClientFactory @Inject constructor(private val client: Client) : Supplier<Dacha2ServiceClient> {

  override fun get(): Dacha2ServiceClient {
    // MR gets its own client with short timeouts so an MR outage is detected quickly - we do not change the
    // shared client used for other things
    val mrClient = ClientBuilder.newBuilder().withConfig(client.configuration).build()
    mrClient.property(ClientProperties.CONNECT_TIMEOUT,
      FallbackPropertyConfig.getConfig("dacha2.redis.mr-connect-timeout-ms", "2000").toInt())
    mrClient.property(ClientProperties.READ_TIMEOUT,
      FallbackPropertyConfig.getConfig("dacha2.redis.mr-read-timeout-ms", "15000").toInt())

    return Dacha2FallbackServiceClient(Dacha2Client(mrClient), redisSource())
  }

  private fun redisSource(): RedisSource {
    val files = ReloadingRedisSource.files(FallbackPropertyConfig.getConfig(Dacha2MRClientFeature.REDIS_CONFIG_FILES))

    if (files.isEmpty()) {
      gaugeConfigured.set(1.0)
      return StaticRedisSource(JedisFeatureHubRedis(RedisConfig.load(Dacha2MRClientFeature.REDIS_CONFIG_PREFIX)!!))
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
