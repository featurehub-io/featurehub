package io.featurehub.party

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientFeature
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.EdgeFeature
import io.featurehub.edge.EdgeResourceFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.health.MetricsHealthRegistration.Companion.registerMetrics
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import io.featurehub.mr.ManagementRepositoryFeature
import io.featurehub.mr.dacha2.Dacha2Feature
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.NATSFeature
import io.featurehub.rest.CacheControlFilter
import io.featurehub.rest.CorsFilter
import io.featurehub.rest.Info.Companion.APPLICATION_NAME_PROPERTY
import io.featurehub.web.security.oauth.OAuth2Feature
import io.featurehub.web.security.saml.SamlEnvironmentalFeature
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
      GoogleEventFeature::class.java,
      CorsFilter::class.java,
      OAuth2Feature::class.java,
      SamlEnvironmentalFeature::class.java,
      ManagementRepositoryFeature::class.java,
      EdgeResourceFeature::class.java,
      EdgeFeature::class.java,
      io.featurehub.dacha2.Dacha2Feature::class.java,
      DachaClientFeature::class.java,
      Dacha2Feature::class.java, // MR API for dacha2
      TelemetryFeature::class.java,
      CacheControlFilter::class.java
    )

    config.register(object: ContainerLifecycleListener {
      override fun onStartup(container: Container) {
        FeatureHubJerseyHost.withServiceLocator(container) { injector ->
          // make sure Edge talks directly to Dacha for the current cache
          val dachaServiceRegistry = injector.getService(DachaClientServiceRegistry::class.java)
          dachaServiceRegistry.registerApiKeyService(name, injector.getService(DachaApiKeyService::class.java) )
        }
      }

      override fun onReload(container: Container?) {}

      override fun onShutdown(container: Container?) {}
    })

    registerMetrics(config)

      // check if we should list on a different port
//    registerMetrics(config) { resourceConfig, locator, binder ->
//      if (resourceConfig != null) {
//        resourceConfig.register(Dacha2Feature::class.java)
//      }
//      resourceConfig
//    }

    FeatureHubJerseyHost(config).start()
    log.info("MR Launched - (HTTP/2 payloads enabled!)")

    Thread.currentThread().join()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Application::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
      System.setProperty("user.timezone", "UTC")
      System.setProperty("dacha1.disabled", "true")
      System.setProperty("dacha2.enabled", "true")
      System.setProperty(APPLICATION_NAME_PROPERTY, "party-server")
      try {
        Application().run()
      } catch (e: Exception) {
        log.error("failed", e)
        System.exit(-1)
      }
    }
  }
}
