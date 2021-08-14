package io.featurehub.dacha

import io.featurehub.dacha.resource.DachaApiKeyResource
import io.featurehub.dacha.resource.DachaEnvironmentResource
import io.featurehub.health.HealthSource
import io.featurehub.lifecycle.BaseLifecycleListener
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener

class DachaFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    arrayOf(
      DachaApiKeyResource::class.java,
      DachaEnvironmentResource::class.java).forEach { clazz -> context.register(clazz) }

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(InMemoryCache::class.java).to(InternalCache::class.java).`in`(Singleton::class.java)
        bind(ServerConfig::class.java).to(ServerConfig::class.java).`in`(Singleton::class.java)
        bind(CacheManager::class.java).to(CacheManager::class.java).to(HealthSource::class.java).`in`(Singleton::class.java)
      }
    })

    context.register(BaseLifecycleListener(CacheManager::class.java))

    return true
  }
}
