package io.featurehub.health

import cd.connect.jersey.common.JerseyPrometheusResource
import io.featurehub.jersey.ApplicationLifecycleListener
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

    fun hasExternalMonitoringPort() = FallbackPropertyConfig.getConfig(monitorPortName) != null

    fun configureMetrics(resourceConfig: ResourceConfig, serviceLocator: ServiceLocator): ResourceConfig {
      // pull all of the health sources out of the main context and replicate them
      // into our health repository
      val healthSources = serviceLocator.getAllServices(HealthSource::class.java);

      listOf(JerseyPrometheusResource::class.java,
        HealthFeature::class.java, LoadBalancerFeature::class.java, ApplicationLifecycleListener::class.java)
        .forEach { resourceConfig.register(it) }

      resourceConfig.register(object: AbstractBinder() {
        override fun configure() {
          healthSources.forEach { hs ->
            if (!(hs is ApplicationHealthSource)) {
              bind(hs).to(HealthSource::class.java)
            }
          }
        }
      })

      return resourceConfig
    }

    fun startMetricsEndpoint(resourceConfig: ResourceConfig) {
      val port = FallbackPropertyConfig.getConfig(monitorPortName)!!.toInt()
      FeatureHubJerseyHost(resourceConfig).disallowWebHosting().start(port)

      log.info("metric/health endpoint now active on port {}", port)
    }

    fun registerMetrics(config: ResourceConfig) {
      registerMetrics(config) { resourceConfig -> resourceConfig }
    }
    /**
     * Register a metrics endpoint against the current port if no external monitoring port is defined, otherwise
     * create a new monitoring port web host, callback for any extra things requiring config and start the internal
     * port.
     */
    fun registerMetrics(config: ResourceConfig, registerCallback: (config: ResourceConfig) -> ResourceConfig) {
      // turn on all jvm prometheus metrics
      DefaultExports.initialize()

      if (FallbackPropertyConfig.getConfig(monitorPortName) == null) {
        config.register(JerseyPrometheusResource::class.java)
        config.register(HealthFeature::class.java)
        config.register(ApplicationLifecycleListener::class.java)

        registerCallback(config)
      } else {
        config.register(object : ContainerLifecycleListener {
          override fun onStartup(container: Container) {

            startMetricsEndpoint(registerCallback(configureMetrics(
              ResourceConfig(), container.applicationHandler
                .injectionManager.getInstance(ServiceLocator::class.java)
            )))
          }

          override fun onReload(container: Container) {
          }

          override fun onShutdown(container: Container) {/*...*/}
        });
      }
    }
  }
}
