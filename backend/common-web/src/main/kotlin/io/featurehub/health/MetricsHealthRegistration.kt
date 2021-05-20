package io.featurehub.health

import cd.connect.jersey.JerseyHttp2Server
import cd.connect.jersey.common.JerseyPrometheusResource
import io.prometheus.client.hotspot.DefaultExports
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This automatically detects if there is a monitor.port setting and if so, pulls the
 * health and prometheus endpoint (/metrics) out of the main Jersey endpoint and waits
 * until that one has fired up and then pulls out the health sources and creates a new
 * resource
 */

class MetricsHealthRegistration {


  companion object {
    private val log: Logger = LoggerFactory.getLogger(MetricsHealthRegistration::class.java)
    const val monitorPortName = "monitor.port"

    fun registerMetrics(config: ResourceConfig) {
      // turn on all jvm prometheus metrics
      DefaultExports.initialize()

      if (System.getProperty(monitorPortName) == null) {
        config.register(JerseyPrometheusResource::class.java)
        config.register(HealthFeature::class.java)
      } else {
        config.register(object : ContainerLifecycleListener {
          override fun onStartup(container: Container) {
            // access the ServiceLocator here
            val injector = container.applicationHandler
              .injectionManager.getInstance(ServiceLocator::class.java)

            // pull all of the health sources out of the main context and replicate them
            // into our health repository
            val healthSources = injector.getAllServices(HealthSource::class.java);

            val resourceConfig = ResourceConfig(JerseyPrometheusResource::class.java, HealthFeature::class.java)

            healthSources.forEach { hs ->
              if (!(hs is ApplicationHealthSource)) {
                resourceConfig.register(hs)
              }
            }

            JerseyHttp2Server().start(
              resourceConfig, System.getProperty(monitorPortName).toInt()
            )

            log.info("metric/health endpoint now active on port {}", System.getProperty(monitorPortName))
          }

          override fun onReload(container: Container) {
          }

          override fun onShutdown(container: Container) {/*...*/}
        });
      }
    }
  }
}
