package io.featurehub.edge

import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.CorsFilter
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import io.featurehub.health.MetricsHealthRegistration.Companion.registerMetrics
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {
  private val log: Logger = LoggerFactory.getLogger(Application::class.java)

  fun run() {
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING)

    DeclaredConfigResolver.resolve(this)

    val config = ResourceConfig(
      CorsFilter::class.java,
      TelemetryFeature::class.java
    )

    registerMetrics(config)

    FeatureHubJerseyHost(config).start()

    log.info("FeatureHub GET-Edge Has Started.")

    Thread.currentThread().join()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Application::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val app = Application()
        app.run()
      } catch (e : Exception) {
        log.error("Failed to start", e)
        System.exit(-1)
      }
    }
  }
}

