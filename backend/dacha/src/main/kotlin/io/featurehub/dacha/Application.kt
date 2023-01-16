package io.featurehub.dacha

import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import io.featurehub.events.CloudEventsFeature
import io.featurehub.health.MetricsHealthRegistration
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import io.featurehub.publish.NATSFeature
import io.featurehub.rest.Info
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.LoggerFactory

object Application {
  private val log = LoggerFactory.getLogger(Application::class.java)
  private fun initializeCommonJerseyLayer() {
    // register our resources, try and tag them as singleton as they are instantiated faster
    val config = ResourceConfig(
      NATSFeature::class.java,
      TelemetryFeature::class.java,
      DachaFeature::class.java,

      CloudEventsFeature::class.java
    )

    // check if we should list on a different port
    MetricsHealthRegistration.registerMetrics(config)
    FeatureHubJerseyHost(config).disallowWebHosting().start()
    log.info("Dacha Launched - (HTTP/2 payloads enabled!)")
  }

  @JvmStatic
  fun main(args: Array<String>) {
    System.setProperty("user.timezone", "UTC")
    System.setProperty(Info.APPLICATION_NAME_PROPERTY, "dacha")
    try {
      initializeCommonJerseyLayer()
      log.info("Cache has started")
      Thread.currentThread().join()
    } catch (e: Exception) {
      log.error("Failed to start", e)
      ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATING)
      System.exit(-1)
    }
  }
}
