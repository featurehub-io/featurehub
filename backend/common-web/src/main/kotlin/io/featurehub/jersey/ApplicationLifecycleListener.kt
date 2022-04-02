package io.featurehub.jersey

import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This triggers the full application lifecycle - only register this on the LAST
 * context if you have multiple contexts. We register this on the Metrics endpoint by
 * default as that is always registered as the second context.
 */
class ApplicationLifecycleListener : ApplicationEventListener {
  private val log: Logger = LoggerFactory.getLogger(ApplicationLifecycleListener::class.java)
  override fun onEvent(event: ApplicationEvent) {
    log.info("application event received {}", event.type)
    if (event.type == ApplicationEvent.Type.INITIALIZATION_FINISHED) {
      if (!ApplicationLifecycleManager.isReady()) {
        log.info("Application started, triggering started events")
        ApplicationLifecycleManager.updateStatus(LifecycleStatus.STARTED)
      }
    } else if (event.type == ApplicationEvent.Type.DESTROY_FINISHED) {
      log.info("Application complete, triggering shutdown events")
      if (ApplicationLifecycleManager.getStatus() != LifecycleStatus.TERMINATED) {
        ApplicationLifecycleManager.updateStatus(LifecycleStatus.TERMINATED)
      }
    }
  }

  override fun onRequest(requestEvent: RequestEvent?): RequestEventListener? {
    return null
  }
}
