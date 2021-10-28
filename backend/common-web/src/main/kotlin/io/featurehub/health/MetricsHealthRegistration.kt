package io.featurehub.health

import cd.connect.jersey.common.JerseyPrometheusResource
import io.featurehub.jersey.FeatureHubJerseyHost
import io.featurehub.utils.FallbackPropertyConfig
import io.prometheus.client.hotspot.DefaultExports
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.internal.inject.AbstractBinder
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

      if (FallbackPropertyConfig.getConfig(monitorPortName) == null) {
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

            resourceConfig.register(object: AbstractBinder() {
              override fun configure() {
                healthSources.forEach { hs ->
                  if (!(hs is ApplicationHealthSource)) {
                    bind(hs).to(HealthSource::class.java)
                  }
                }
              }
            })

            val port = FallbackPropertyConfig.getConfig(monitorPortName)!!.toInt()
            FeatureHubJerseyHost(resourceConfig).start(port)

            log.info("metric/health endpoint now active on port {}", port)
          }

          override fun onReload(container: Container) {
          }

          override fun onShutdown(container: Container) {/*...*/}
        });
      }
    }
  }
}
