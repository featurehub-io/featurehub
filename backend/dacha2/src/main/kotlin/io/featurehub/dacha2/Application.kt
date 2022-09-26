package io.featurehub.dacha2

import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import io.featurehub.dacha2.client.Dacha2MRClientFeature
import io.featurehub.events.CloudEventsFeature
import io.featurehub.events.kinesis.KinesisEventFeature
import io.featurehub.events.pubsub.GoogleEventFeature
import io.featurehub.health.CommonFeatureHubFeatures
import io.featurehub.health.MetricsHealthRegistration
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.lifecycle.TelemetryFeature
import io.featurehub.publish.NATSFeature
import io.featurehub.rest.Info
import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {
  private val log: Logger = LoggerFactory.getLogger(Application::class.java)

  fun run() {
    ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTING)

    // ensure migrations do not run
//    System.setProperty("db.run-migrations", "false")

    val config = ResourceConfig(
      CommonFeatureHubFeatures::class.java,
      TelemetryFeature::class.java,
      Dacha2Feature::class.java,
      CloudEventsFeature::class.java,
      GoogleEventFeature::class.java,
      NATSFeature::class.java,
      KinesisEventFeature::class.java,
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
