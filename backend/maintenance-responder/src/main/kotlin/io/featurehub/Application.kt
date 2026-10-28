package io.featurehub

import io.featurehub.health.MetricsHealthRegistration.Companion.registerMetrics
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.lifecycle.ApplicationLifecycleManager.Companion.updateStatus
import io.featurehub.lifecycle.LifecycleStatus
import io.featurehub.responder.AbortForMaintenanceFilter
import io.featurehub.rest.CorsFilter
import io.featurehub.rest.Info
import io.featurehub.rest.MaintenanceNotificationFilter
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class Application {
  companion object {
    val log: Logger = LoggerFactory.getLogger(Application::class.java)

    @JvmStatic
    fun main(args: Array<String>) {
      System.setProperty("user.timezone", "UTC")
      System.setProperty(Info.APPLICATION_NAME_PROPERTY, "maintenance-responder")

      try {
        Application().run()
      } catch (e: Exception) {
        log.error("failed", e)
        // force any running Jersey context's to terminate
        updateStatus(LifecycleStatus.TERMINATING)
        exitProcess(1)
      }
    }
  }

  @Throws(Exception::class)
  private fun run() {
    if (!MaintenanceNotificationFilter.wireFilterCheck()) {
      log.error("There is no configured maintenance window, please set one before starting this app.")
      throw RuntimeException("There is no configured maintenance window")
    }

    // register our resources, try and tag them as singleton as they are instantiated faster
    val config = ResourceConfig(
      CorsFilter::class.java,
      CommonConfiguration::class.java,
      AbortForMaintenanceFilter::class.java,
    )

    registerMetrics(config) { resourceConfig: ResourceConfig?, locator: ServiceLocator?, binder: AbstractBinder? ->
      resourceConfig
    }

    FeatureHubJerseyHost(config).start()

    log.info("Maintenance Responder Launched - (HTTP/2 payloads enabled!)")

    Thread.currentThread().join()
  }
}
