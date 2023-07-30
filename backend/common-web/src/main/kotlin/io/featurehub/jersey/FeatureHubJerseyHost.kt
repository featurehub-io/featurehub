package io.featurehub.jersey

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import cd.connect.lifecycle.LifecycleTransition
import io.featurehub.health.CommonFeatureHubFeatures
import io.featurehub.lifecycle.ExecutorPoolDrainageSource
import io.featurehub.utils.FallbackPropertyConfig
import org.glassfish.grizzly.http.server.HttpHandlerChain
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

  var allowedWebHosting = true
  private var jerseyPrefixes = listOf("/mr-api/*", "/saml/*", "/oauth/*", "/f2", "/f2/*", "/features", "/features/*", "/info/*", "/dacha2/*")

  init {
    DeclaredConfigResolver.resolve(this)

    var prefixes = FallbackPropertyConfig.getConfig("jersey.prefixes")

    if (prefixes != null) {
      jerseyPrefixes = prefixes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }

    config.register(
      CommonFeatureHubFeatures::class.java,
    ).register(object : ContainerLifecycleListener {
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
    /**
     * We cannot use ContainerLifecycleListener as a generic callback as we don't know what container stuff is going
     * into so we can't glob them together and you cannot register the same classname > 1 time with the process. Always
     * use the ContainerLifecycleListener in-situ and use this function if you want the service locator. If you want services
     * to be available at startup, use @Immediate
     */
    fun withServiceLocator(container: Container, locate: (serviceLocator: ServiceLocator) -> Unit) {
      val serviceLocator = container
        .applicationHandler
        .injectionManager
        .getInstance(ServiceLocator::class.java)

      locate(serviceLocator)
    }
  }

  fun disallowWebHosting(): FeatureHubJerseyHost {
    allowedWebHosting = false
    return this
  }

  fun start(): FeatureHubJerseyHost {
    return start(port!!)
  }

  fun start(overridePort: Int): FeatureHubJerseyHost {
    val offsetPath = FallbackPropertyConfig.getConfig("featurehub.url-path", "/")
    val BASE_URI = URI.create(String.format("http://0.0.0.0:%d%s", overridePort, offsetPath))
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

    val handlerChain = HttpHandlerChain(server)
    val resourceHandler =  HttpGrizzlyContainer.makeHandler(config)

    val contextPath: String =
      if (offsetPath.endsWith("/")) offsetPath.substring(0, offsetPath.length - 1) else offsetPath

    val amHosting = (allowedWebHosting && FallbackPropertyConfig.getConfig("run.nginx") != null)
    if (amHosting) {
      log.info("starting with web asset support - adding default path")

      handlerChain.addHandler(DelegatingHandler(AdminAppStaticHttpHandler(offsetPath)),
          arrayOf(HttpHandlerRegistration.Builder().contextPath(contextPath).urlPattern("").build()))
    }

    val resourceHandlerPatterns = mutableListOf<HttpHandlerRegistration>()
    // we have been mounted to respond at /foo/featurehub, so we need to mount and response at / as well (secondarily)
    if (contextPath != "/") {
      resourceHandlerPatterns.add(HttpHandlerRegistration.Builder().contextPath("").urlPattern("/metrics").build())
      resourceHandlerPatterns.add(HttpHandlerRegistration.Builder().contextPath("").urlPattern("/health/*").build())

      if (jerseyPrefixes.contains("/dacha2/*")) {
        resourceHandlerPatterns.add(HttpHandlerRegistration.Builder().contextPath("").urlPattern("/dacha2/*").build())
      }
    }

    resourceHandlerPatterns.add(HttpHandlerRegistration.Builder().contextPath(contextPath).urlPattern("/metrics").build())
    resourceHandlerPatterns.add(HttpHandlerRegistration.Builder().contextPath(contextPath).urlPattern("/health/*").build())

    jerseyPrefixes.forEach { prefix ->
      resourceHandlerPatterns.add(HttpHandlerRegistration.Builder().contextPath(contextPath).urlPattern(prefix).build())
    }

    handlerChain.addHandler(resourceHandler, resourceHandlerPatterns.toTypedArray())

    if (amHosting) {
      log.info("starting with web asset support - adding remaining paths")

      handlerChain.addHandler(DelegatingHandler(AdminAppStaticHttpHandler(offsetPath)),
        arrayOf(HttpHandlerRegistration.Builder().contextPath(contextPath).urlPattern("/*").build()))

//      val assetHandlingPatterns  = mutableListOf<HttpHandlerRegistration>()
//
//      listOf("*").forEach {
//        assetHandlingPatterns.add(HttpHandlerRegistration.Builder().contextPath(contextPath).urlPattern(it).build())
//      }
//
//      handlerChain.addHandler(DelegatingHandler(AdminAppStaticHttpHandler(offsetPath)), assetHandlingPatterns.toTypedArray())
    }


    serverConfig.addHttpHandler(handlerChain)
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
