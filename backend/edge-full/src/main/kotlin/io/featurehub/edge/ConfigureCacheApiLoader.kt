package io.featurehub.edge

import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.dacha.api.NoSuchCacheDachaClient
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@LifecyclePriority(priority = 2)
class ConfigureCacheApiLoader @Inject constructor(private val registry: DachaClientServiceRegistry) : LifecycleStarted {
  private val log: Logger = LoggerFactory.getLogger(ConfigureCacheApiLoader::class.java)

  override fun started() {
    val cacheName = FallbackPropertyConfig.getMandatoryConfig("cache.name")
    if (registry.getApiKeyService(cacheName) is NoSuchCacheDachaClient) {
      log.error(
        "You must configure the URL indicating where dacha is located. dacha.url.{} is missing",
        cacheName
      )

      throw RuntimeException("Cannot find dacha url, see error log")
    }
  }
}
