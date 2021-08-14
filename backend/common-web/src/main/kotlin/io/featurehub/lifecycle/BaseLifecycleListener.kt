package io.featurehub.lifecycle

import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.spi.Container
import org.glassfish.jersey.server.spi.ContainerLifecycleListener

/**
 * We generally don't do anything with the lifecycle apart from
 * grab the injector and preload classes
 */
open class BaseLifecycleListener(vararg private val postStartupLoadServices: Class<*>) : ContainerLifecycleListener {
  override fun onStartup(container: Container) {
    // access the ServiceLocator here
    val injector = container
      .applicationHandler
      .injectionManager
      .getInstance(ServiceLocator::class.java)

    postStartupLoadServices.forEach { injector.getService(it) }

    withInjector(injector)
  }

  open fun withInjector(injector: ServiceLocator) {
  }

  override fun onReload(container: Container) {

  }

  override fun onShutdown(container: Container) {

  }
}
