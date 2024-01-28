package io.featurehub.dacha2

import io.featurehub.dacha2.client.Dacha2MRClientFeature
import io.featurehub.events.CloudEventsFeature
import io.featurehub.health.CommonFeatureHubFeatures
import io.featurehub.health.MetricsHealthRegistration
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import io.featurehub.rest.Info
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {
  private val log: Logger = LoggerFactory.getLogger(Application::class.java)

  fun run() {
    // ensure migrations do not run
//    System.setProperty("db.run-migrations", "false")

    val config = ResourceConfig(
      CommonFeatureHubFeatures::class.java,
      TelemetryFeature::class.java,
      Dacha2Feature::class.java,
      CloudEventsFeature::class.java,
      Dacha2MRClientFeature::class.java, // the Api to the MR instance, (party-server mocks them)
    )

    // recommended this is on a different port
    MetricsHealthRegistration.registerMetrics(config)

    FeatureHubJerseyHost(config).disallowWebHosting().start()

    log.info("FeatureHub Dacha2 Has Started.")

    Thread.currentThread().join()
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(Application::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
      System.setProperty("user.timezone", "UTC")
      System.setProperty(Info.APPLICATION_NAME_PROPERTY, "dacha2")
      System.setProperty("dacha2.enabled", "true")


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
