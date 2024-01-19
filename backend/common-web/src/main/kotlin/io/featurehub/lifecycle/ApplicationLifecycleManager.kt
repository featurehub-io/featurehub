package io.featurehub.lifecycle

import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer

enum class LifecycleStatus {
  STARTING,
  STARTED,
  TERMINATING,
  TERMINATED
}

class LifecycleTransition(val current: LifecycleStatus, val next: LifecycleStatus, val appEvent: ApplicationEvent?)

class ApplicationLifecycleManager {
  companion object {
    private var savedAppEvent: ApplicationEvent? = null
    private var status: LifecycleStatus = LifecycleStatus.STARTING
    private var stateChangeListeners: MutableList<Consumer<LifecycleTransition>> = mutableListOf()
    private val log: Logger = LoggerFactory.getLogger(ApplicationLifecycleManager::class.java)

    init {
      Runtime.getRuntime().addShutdownHook(Thread {
        updateStatus(LifecycleStatus.TERMINATING)
      })
    }

    fun getStatus(): LifecycleStatus {
      return status
    }

    fun registerListener(trans: Consumer<LifecycleTransition>) {
      stateChangeListeners.add(trans)
    }

    fun updateStatus(newStatus: LifecycleStatus, appEvent: ApplicationEvent?) {
      log.debug("Setting status to {}", newStatus)
      savedAppEvent = appEvent ?: savedAppEvent
      val trans = LifecycleTransition(status, newStatus, savedAppEvent)
      val listeners = stateChangeListeners.toList()

      listeners.forEach { s ->
        try {
          s.accept(trans)
        } catch (var4: RuntimeException) {
          log.error("Failed to notify of state change: {}", trans, var4)
          if (newStatus == LifecycleStatus.STARTED) {
            System.exit(-1)
          }
        } catch (var5: Exception) {
          log.error("Failed to notify of state change: {}", trans, var5)
        }
      }

      status = newStatus
    }

    fun updateStatus(newStatus: LifecycleStatus) {
      updateStatus(newStatus, null)
    }

    fun isAlive(): Boolean {
      return status == LifecycleStatus.STARTING || status == LifecycleStatus.STARTED
    }

    fun isReady(): Boolean {
      return status == LifecycleStatus.STARTED
    }
  }
}
