package io.featurehub.dacha2.client

import cd.connect.openapi.support.ApiClient
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.dacha2.api.impl.Dacha2ServiceServiceImpl
import io.featurehub.redis.JedisFeatureHubRedis
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

    context.register(object: AbstractBinder() {
      override fun configure() {
        if (redisConfig == null) {
          bind(Dacha2Client::class.java).to(Dacha2ServiceClient::class.java).`in`(Immediate::class.java)
        } else {
          log.info("dacha2: redis fallback for management repository outages is enabled ({})", redisConfig)
          bindFactory(Dacha2FallbackClientFactory::class.java, Singleton::class.java).to(Dacha2ServiceClient::class.java).`in`(Singleton::class.java)
        }
      }
    })

    return true
  }

  companion object {
    const val REDIS_CONFIG_PREFIX = "dacha2.redis"
    private val log: Logger = LoggerFactory.getLogger(Dacha2MRClientFeature::class.java)
  }
}

class Dacha2Client @Inject constructor(client: Client) : Dacha2ServiceServiceImpl(ApiClient(client,
  FallbackPropertyConfig.getMandatoryConfig("management-repository.url")
))

class Dacha2FallbackClientFactory @Inject constructor(private val client: Client) : Supplier<Dacha2ServiceClient> {
  private val redisConfig = RedisConfig.load(Dacha2MRClientFeature.REDIS_CONFIG_PREFIX)!!

  override fun get(): Dacha2ServiceClient {
    // MR gets its own client with short timeouts so an MR outage is detected quickly - we do not change the
    // shared client used for other things
    val mrClient = ClientBuilder.newBuilder().withConfig(client.configuration).build()
    mrClient.property(ClientProperties.CONNECT_TIMEOUT,
      FallbackPropertyConfig.getConfig("dacha2.redis.mr-connect-timeout-ms", "2000").toInt())
    mrClient.property(ClientProperties.READ_TIMEOUT,
      FallbackPropertyConfig.getConfig("dacha2.redis.mr-read-timeout-ms", "15000").toInt())

    return Dacha2FallbackServiceClient(Dacha2Client(mrClient), JedisFeatureHubRedis(redisConfig))
  }
}
