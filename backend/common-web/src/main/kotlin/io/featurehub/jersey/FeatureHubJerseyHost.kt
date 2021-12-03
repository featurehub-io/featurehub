package io.featurehub.jersey

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import cd.connect.lifecycle.LifecycleTransition
import io.featurehub.health.CommonFeatureHubFeatures
import io.featurehub.lifecycle.ExecutorPoolDrainageSource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.core.Configurable
import org.glassfish.grizzly.http.server.HttpHandlerRegistration
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.http.server.NetworkListener
import org.glassfish.grizzly.http2.Http2AddOn
import org.glassfish.grizzly.utils.Charsets
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.grizzly2.httpserver.HttpGrizzlyContainer
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

class FeatureHubJerseyHost constructor(private val config: ResourceConfig) {
  private val log: Logger = LoggerFactory.getLogger(FeatureHubJerseyHost::class.java)

  @ConfigKey("server.port")
  var port: Int? = 8903

  @ConfigKey("server.gracePeriodInSeconds")
  var gracePeriod: Int? = 10

  init {
    DeclaredConfigResolver.resolve(this)

    config.register(
      CommonFeatureHubFeatures::class.java,
    ).register(object: ContainerLifecycleListener {
      override fun onStartup(container: Container) {
        // access the ServiceLocator here
        val injector = container
          .applicationHandler
          .injectionManager
          .getInstance(ServiceLocator::class.java)

        val drainSources = injector.getAllServices(ExecutorPoolDrainageSource::class.java)

        ApplicationLifecycleManager.registerListener { trans ->
          if (trans.next == LifecycleStatus.TERMINATING) {
            for (drainSource in drainSources) {
              drainSource.drain()
            }
          }
        }
      }

      override fun onReload(container: Container) {
      }

      override fun onShutdown(container: Container) {
      }
    })
  }

  companion object {
    private val log: Logger = LoggerFactory.getLogger(FeatureHubJerseyHost::class.java)
    /**
     * This is a convenient way of making sure certain services exist. Because it creates an abstract object
     * you can create as many of these as you like. If you use a common class and register it multiple times, it
     * will only create the first instance.
     */
    fun registerServiceToLoadOnStart(config: Configurable<*>, vararg postStartupLoadServices: Class<*>) {
      config.register(object: ContainerLifecycleListener {
        override fun onStartup(container: Container) {
          // access the ServiceLocator here
          val injector = container
            .applicationHandler
            .injectionManager
            .getInstance(ServiceLocator::class.java)

          var failedToFindServices = false

          for (it in postStartupLoadServices) {
            if (injector.getService(it) == null) {
              log.error("Unable to find service with class {}", it.name)
              failedToFindServices = true
            }
          }

          if (failedToFindServices) {
            throw RuntimeException("Incomplete wiring due to inability to find services, please check errors")
          }
        }

        override fun onReload(container: Container) {
        }

        override fun onShutdown(container: Container) {
        }

      })
    }

    /**
     * This is a convenient method of grabbing the Injection Context and doing things with it
     * once the container has started
     */
    fun withInjector(config: Configurable<*>, withInjectionContext: (injector: ServiceLocator) -> Unit ) {
     config.register(object: ContainerLifecycleListener {
       override fun onStartup(container: Container) {
         // access the ServiceLocator here
         val injector = container
           .applicationHandler
           .injectionManager
           .getInstance(ServiceLocator::class.java)

         withInjectionContext(injector)
       }

       override fun onReload(container: Container) {
       }

       override fun onShutdown(container: Container) {
       }
     })
   }
  }

  fun start() : FeatureHubJerseyHost {
    return start(port!!)
  }

  fun start(overridePort: Int) : FeatureHubJerseyHost {
    val BASE_URI = URI.create(String.format("http://0.0.0.0:%d/", overridePort))
//    val server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false)

    val listener = NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, overridePort)

    listener.transport.workerThreadPoolConfig.threadFactory = ThreadFactoryBuilder()
      .setNameFormat("grizzly-http-server-%d")
      .setUncaughtExceptionHandler(JerseyProcessingUncaughtExceptionHandler())
      .build()

    listener.registerAddOn(Http2AddOn())

    val server = HttpServer()
    server.addListener(listener)

    val serverConfig = server.serverConfiguration

    val resourceHandler = HttpGrizzlyContainer.makeHandler(config)

    if (FallbackPropertyConfig.getConfig("run.nginx") != null) {
      log.info("starting with web asset support")
      serverConfig.addHttpHandler(DelegatingHandler(resourceHandler, AdminAppStaticHttpHandler()))
    } else {
      serverConfig.addHttpHandler(resourceHandler)
    }


    serverConfig.isPassTraceRequest = true
    serverConfig.defaultQueryEncoding = Charsets.UTF8_CHARSET

    ApplicationLifecycleManager.registerListener { trans: LifecycleTransition ->
      if (trans.next == LifecycleStatus.TERMINATING) {
        try {
          server.shutdown(gracePeriod!!.toLong(), TimeUnit.SECONDS).get()
        } catch (e: InterruptedException) {
          log.error("Failed to shutdown server in {} seconds", gracePeriod)
        } catch (e: ExecutionException) {
          log.error("Failed to shutdown server in {} seconds", gracePeriod)
        }
      }
    }

    server.start()

    log.info("server started on {} with http/2 enabled", BASE_URI.toString())

    return this
  }
}
