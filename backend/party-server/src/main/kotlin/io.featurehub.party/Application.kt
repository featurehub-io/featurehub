package io.featurehub.party

import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaClientFeature
import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.edge.EdgeFeature
import io.featurehub.edge.EdgeResourceFeature
import io.featurehub.events.CloudEventConfigDiscoveryService
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.PubsubEventFeature
import io.featurehub.health.MetricsHealthRegistration.Companion.registerMetrics
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.LifecycleListeners
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.lifecycle.TelemetryFeature
import io.featurehub.mr.ManagementRepositoryFeature
import io.featurehub.mr.dacha2.Dacha2Feature
import io.featurehub.publish.ChannelConstants
import io.featurehub.publish.NATSFeature
import io.featurehub.rest.CacheControlFilter
import io.featurehub.rest.CorsFilter
import io.featurehub.rest.Info.Companion.APPLICATION_NAME_PROPERTY
import io.features.webhooks.features.WebhookFeature
import jakarta.inject.Inject
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@LifecyclePriority(priority = 2)
class PartyInjectorStarter @Inject constructor(
  private val dachaServiceRegistry: DachaClientServiceRegistry,
  private val apiKeyService: DachaApiKeyService) : LifecycleStarted {
  override fun started() {
    // make sure Edge talks internally to Dacha for the current cache
    dachaServiceRegistry.registerApiKeyService(ChannelConstants.DEFAULT_CACHE_NAME, apiKeyService )
  }
}

class Application {
  private val log = LoggerFactory.getLogger(io.featurehub.Application::class.java)

  @Throws(Exception::class)
  private fun run() {
    // register our resources, try and tag them as singleton as they are instantiated faster
    val config = ResourceConfig(
      NATSFeature::class.java,
      PubsubEventFeature::class.java,
      KinesisEventFeature::class.java,
      CorsFilter::class.java,
      ManagementRepositoryFeature::class.java,
      EdgeResourceFeature::class.java,
      EdgeFeature::class.java,
      io.featurehub.dacha2.Dacha2Feature::class.java,
      DachaClientFeature::class.java,
      Dacha2Feature::class.java, // MR API for dacha2
      TelemetryFeature::class.java,
      CacheControlFilter::class.java,
    )

    if (WebhookFeature.enabled) {
      config.register(WebhookFeature::class.java)
    }

    LifecycleListeners.starter(PartyInjectorStarter::class.java, config)

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
      System.setProperty("dacha1.enabled", "false")
      System.setProperty("dacha2.enabled", "true")
      System.setProperty(APPLICATION_NAME_PROPERTY, "party-server")

      CloudEventConfigDiscoveryService.addTags("dacha2", "edge", "mr")

      try {
        Application().run()
      } catch (e: Exception) {
        log.error("failed", e)
        System.exit(-1)
      }
    }
  }
}
