package io.featurehub.party

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.CorsFilter
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import io.featurehub.dacha.DachaFeature
import io.featurehub.dacha.api.DachaClientFeature
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.dacha.resource.DachaApiKeyResource
import io.featurehub.edge.EdgeFeature
import io.featurehub.edge.EdgeResourceFeature
import io.featurehub.health.MetricsHealthRegistration.Companion.registerMetrics
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import io.featurehub.mr.ManagementRepositoryFeature
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.NATSFeature
import io.featurehub.web.security.oauth.OAuth2Feature
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {
  private val log = LoggerFactory.getLogger(io.featurehub.Application::class.java)

  @ConfigKey("cache.name")
  var name = ChannelConstants.DEFAULT_CACHE_NAME

  init {
    DeclaredConfigResolver.resolve(this)
  }

  @Throws(Exception::class)
  private fun run() {
    // register our resources, try and tag them as singleton as they are instantiated faster
    val config = ResourceConfig(
      NATSFeature::class.java,
      CorsFilter::class.java,
      OAuth2Feature::class.java,
      ManagementRepositoryFeature::class.java,
      EdgeResourceFeature::class.java,
      EdgeFeature::class.java,
      DachaFeature::class.java,
      DachaClientFeature::class.java,
      TelemetryFeature::class.java
    )

    FeatureHubJerseyHost.withInjector(config) { injector ->
      // make sure Edge talks directly to Dacha for the current cache
      val dachaServiceRegistry = injector.getService(DachaClientServiceRegistry::class.java)
      dachaServiceRegistry.registerApiKeyService(name, injector.getService(DachaApiKeyResource::class.java) )
    }

      // check if we should list on a different port
    registerMetrics(config)
    FeatureHubJerseyHost(config).start()
    log.info("MR Launched - (HTTP/2 payloads enabled!)")

    Thread.currentThread().join()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Application::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
      System.setProperty("user.timezone", "UTC")
      try {
        Application().run()
      } catch (e: Exception) {
        log.error("failed", e)
        System.exit(-1)
      }
    }
  }
}
