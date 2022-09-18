package io.featurehub.dacha.api

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.LoggingConfiguration
import cd.connect.openapi.support.ApiClient
import io.featurehub.dacha.api.impl.DachaApiKeyServiceServiceImpl
import io.featurehub.dacha.api.impl.DachaEnvironmentServiceServiceImpl
import io.featurehub.dacha.model.DachaKeyDetailsResponse
import io.featurehub.dacha.model.DachaPermissionResponse
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.publish.NATSSource
import io.featurehub.utils.FallbackPropertyConfig
import io.prometheus.client.Counter
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import org.glassfish.jersey.client.ClientProperties
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class NoSuchCacheDachaClient : DachaApiKeyService {
  override fun getApiKeyDetails(eId: UUID, serviceAccountKey: String, excludeRetired: Boolean?): DachaKeyDetailsResponse {
    throw NotFoundException()
  }

  override fun getApiKeyPermissions(eId: UUID, serviceAccountKey: String, featureKey: String): DachaPermissionResponse {
    throw NotFoundException()
  }
}

interface BackupDachaFactory {
  fun supportsNATS(): Boolean
  fun createClient(cacheName: String, readTimeout: Long): NATSDachaApiKeyService
}

class NoNatsBackupDachaFactory : BackupDachaFactory {
  override fun supportsNATS(): Boolean {
    return false
  }

  override fun createClient(cacheName: String, readTimeout: Long): NATSDachaApiKeyService {
    throw NotImplementedError()
  }
}

class NatsBackupDachaFactory @Inject constructor(private val natsSource: NATSSource) : BackupDachaFactory {
  override fun supportsNATS(): Boolean {
    return true
  }

  override fun createClient(cacheName: String, readTimeout: Long): NATSDachaApiKeyService {
    return NATSDachaApiKeyService(natsSource, cacheName, readTimeout)
  }
}

class DachaClientRegistry @Inject constructor(private val backupDachaFactory: BackupDachaFactory) : DachaClientServiceRegistry {
  private val client: Client
  private val environmentServiceMap: MutableMap<String, DachaEnvironmentService> = ConcurrentHashMap()
  private val apiServiceMap: MutableMap<String, DachaApiKeyService> = ConcurrentHashMap()
  private val cacheHitCounter = Counter.Builder().name("dacha_client_cache_hit")
    .help("Number of times cache was hit when requesting dacha client").register()
  private val cacheMissCounter = Counter.Builder().name("dacha_client_cache_miss")
    .help("Number of times the cache was missed when requesting dacha client").register()
  private val notFoundCache = NoSuchCacheDachaClient()

  @ConfigKey("dacha.timeout.connect")
  var connectTimeout: Int? = 4000

  @ConfigKey("dacha.timeout.read")
  var readTimeout: Int? = 4000

  init {
    DeclaredConfigResolver.resolve(this)
    client = ClientBuilder.newClient()
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)
    client.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
    client.property(ClientProperties.READ_TIMEOUT, readTimeout)
  }

  private fun url(cache: String): String? {
    return FallbackPropertyConfig.getConfig("$DACHA_URL$cache")
  }

  private fun noCacheConfigurationReferences(): Boolean {
    return System.getProperties().keys.none { k -> k is String && k.startsWith(DACHA_URL)}
      && System.getenv().keys.none { k-> k.startsWith(DACHA_URL)}
  }

  override fun getApiKeyService(cache: String): DachaApiKeyService {
    val cacheName = cache.lowercase(Locale.getDefault())
    var service = apiServiceMap[cacheName]

    if (service == null) {
      val url = url(cacheName)

      if (url != null) {
        service = DachaApiKeyServiceServiceImpl(ApiClient(client, url))
        apiServiceMap[cacheName] = service
      } else if (noCacheConfigurationReferences() && backupDachaFactory.supportsNATS() ) {
        log.info("Cache {} is not configured by REST definition, resorting back to NATS Request/Reply", cacheName)
        service = backupDachaFactory.createClient(cacheName, readTimeout!!.toLong())
        apiServiceMap[cacheName] = service
      } else {
        log.error("Request for missing cache {}", cacheName)
      }
    }

    if (service == null) {
      service = notFoundCache
      apiServiceMap[cacheName] = service

      cacheMissCounter.inc()
    } else {
      cacheHitCounter.inc()
    }

    return service
  }

  override fun registerApiKeyService(cache: String, apiKeyService: DachaApiKeyService) {
    apiServiceMap[cache] = apiKeyService
  }

  override fun getEnvironmentService(cache: String): DachaEnvironmentService? {
    var cacheName = cache
    cacheName = cacheName.lowercase(Locale.getDefault())
    var service = environmentServiceMap[cacheName]
    if (service == null) {
      val url = url(cacheName)
      if (url != null) {
        service = DachaEnvironmentServiceServiceImpl(ApiClient(client, url))
        environmentServiceMap[cacheName] = service
      }
    }
    if (service == null) {
      cacheMissCounter.inc()
    } else {
      cacheHitCounter.inc()
    }
    return service
  }

  companion object {
    private val log = LoggerFactory.getLogger(DachaClientRegistry::class.java)

    private const val DACHA_URL = "dacha.url."
  }
}
