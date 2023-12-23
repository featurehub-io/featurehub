package io.featurehub.lifecycle

/**
 * This exists as a way of providing a method of post-initializating and pre-shutdown for the whole platform. It gives
 * an ordered priority over the two methods that are built into HK2 (i.e. Immediate (now in some random order) and Singleton (later)).
 *
 * It also gives knowledge of when the process is complete. You should try and avoid writing your own ContainerLifecycleListeners as they
 * will interfere with the process here.
 *
 * Lifecycle shutdown or startup listeners should always be EDGE classes, they are never registered in the service context and
 * are not findable.
 */

import io.featurehub.utils.ExecutorUtil
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Configurable
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Singleton
annotation class LifecyclePriority(val priority: Int) {
  companion object {
    const val APPLICATION_PRIORITY_START = 10
    const val APPLICATION_PRIORITY_END = 100
    const val CRITICAL_INTERNAL_PRIORITY_START = 0
    const val CRITICAL_INTERNAL_PRIORITY_END = 4
    const val INTERNAL_PRIORITY_START = 5
    const val INTERNAL_PRIORITY_END = 9
  }
}

/**
 * implement this if you just want your class to be instantiated
 */
interface LifecycleListener {
}

/**
 * implement this if you want to get a started callback
 */
interface LifecycleStarted : LifecycleListener {
  fun started()
}

interface LifecycleShutdown : LifecycleListener {
  fun shutdown()
}

class ApplicationStarted {
  var status = LifecycleStatus.STARTING
}

// should happen LAST
@LifecyclePriority(priority = Int.MAX_VALUE)
class ApplicationStartedListener @Inject constructor(private val app: ApplicationStarted) : LifecycleStarted {
  override fun started() {
    app.status = LifecycleStatus.STARTED
  }
}

@LifecyclePriority(priority = 0)
class ApplicationShutdownListener @Inject constructor(private val app: ApplicationStarted) : LifecycleShutdown {
  override fun shutdown() {
    app.status = LifecycleStatus.TERMINATING
  }
}

class LifecycleListenerFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(LifecycleListeners::class.java)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(ApplicationStarted::class.java).to(ApplicationStarted::class.java).`in`(Singleton::class.java)
      }
    })

    LifecycleListeners.starter(ApplicationStartedListener::class.java, context)
    LifecycleListeners.shutdown(ApplicationShutdownListener::class.java, context)

    return true
  }
}

/**
 * Objects are never (AFAIK) bound into the current service location, so they should never be classes that expect to be used by
 * other classes
 */
class LifecycleListenerProcessor<T : LifecycleListener>(
  private val clazz: Class<T>, private val container: Container,private val allowFailure: Boolean, private val propertyName: String) {
  private val log: Logger = LoggerFactory.getLogger(LifecycleListenerProcessor::class.java)
  var failed: Boolean = false
  private val serviceLocator = container.applicationHandler.injectionManager.getInstance(ServiceLocator::class.java)!!
  val existingListeners = mutableMapOf<Class<T>,T>()

  fun process() {
    if (!container.configuration.hasProperty(propertyName)) {
      return
    }

    val collected = mutableMapOf<Int,MutableList<Class<T>>>()
    // we get the service handlers, not the ACTUAL services, as getting the service will initialize them, which will
    // be in a random order.
    (container.configuration.properties[propertyName] as List<Class<T>>).forEach { handleClazz: Class<T> ->
      var priority = 100
      handleClazz.getAnnotation(LifecyclePriority::class.java)?.let {
        priority = it.priority
      }

      collected.getOrPut(priority) { mutableListOf() }.add(handleClazz)
    }

    val maxThreads = collected.values.map { it.size }.max()

    // we create an executor just for this process, even if the max threads is 1
    val executor = ExecutorUtil().executorService(maxThreads)

    failed = false
    try {
      for (key in collected.keys.sorted()) {
        if (failed && !allowFailure) {
          break
        }
        collected[key]?.let { handles ->
          if (handles.size == 1) {
            run(handles[0])
          } else {
            spread(handles, executor)
          }
        }
      }
    } finally {
      executor.shutdown()
    }

    if (failed && !allowFailure) {
      log.error("Lifecycle failed for {}", clazz.name)
      throw RuntimeException("Lifecycle failed for ${clazz.name}")
    }
  }

  private fun spread(handles: List<Class<T>>, executor: ExecutorService) {
    val futures = handles.map { CompletableFuture.runAsync({ -> run(it) }, executor ) }.toTypedArray()

    CompletableFuture.allOf(*futures).get()
  }

  private fun run(handle: Class<T>) {
    try {
      log.info("starting {}", handle.name)
      val created = existingListeners[handle] ?: serviceLocator.createAndInitialize(handle)

      if (created is LifecycleStarted && propertyName == LifecycleListeners.START_KEY) {
        created.started()
      } else if (created is LifecycleShutdown && propertyName == LifecycleListeners.SHUTDOWN_KEY) {
        created.shutdown()
      }

      if (created is LifecycleListener) {
        existingListeners[handle] = created
      }
    } catch (e: Exception) {
      log.error("failed to run {}", handle.name, e)

      if (!allowFailure) {
        failed = true // we could be inside a parallel set of tasks, so we need to bubble this out
      }
    }
  }
}
/**
 * We have this because the Immediate annotation does not allow for priority based immediacy. At times we need
 * services to start immediately, but in a different priority order.
 */
class LifecycleListeners : ContainerLifecycleListener {
  override fun onStartup(container: Container) {
    LifecycleListenerProcessor(LifecycleListener::class.java, container, false, START_KEY).process()
  }

  override fun onReload(container: Container) {
  }

  override fun onShutdown(container: Container) {
    LifecycleListenerProcessor(LifecycleListener::class.java, container, true, SHUTDOWN_KEY).process()
  }

  companion object {
    internal const val START_KEY = "lifecycle.start"
    internal const val SHUTDOWN_KEY = "lifecycle.shutdown"

    /**
     * We use the properties here to just store the class names because we cannot register an abstract binding or similar
     * multiple times. The class name generated from an object: AbstractBinder is always the same and Jersey is picking it up
     * as the same class and not letting us register > 1 thing.
     */
    private fun <T: LifecycleListener> register(clazz: Class<T>, config: Configurable<*>, key: String) {
      var bindings = if (config.configuration.hasProperty(key)) config.configuration.properties[key] as MutableList<Class<T>> else null
      if (bindings == null) {
        bindings = mutableListOf()

        config.property(key, bindings)
      }

      bindings.add(clazz)
    }
    fun <T: LifecycleListener> starter(clazz: Class<T>, config: Configurable<*>) {
      register(clazz, config, START_KEY)
    }

    fun <T: LifecycleListener> shutdown(clazz: Class<T>, config: Configurable<*>) {
      register(clazz, config, SHUTDOWN_KEY)
    }

    fun <T: LifecycleListener> wrap(clazz: Class<T>, config: Configurable<*>) {
      register(clazz, config, START_KEY)
      register(clazz, config, SHUTDOWN_KEY)
    }
  }
}
