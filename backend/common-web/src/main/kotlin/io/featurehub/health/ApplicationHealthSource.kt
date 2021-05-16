package io.featurehub.health

import cd.connect.lifecycle.ApplicationLifecycleManager
import cd.connect.lifecycle.LifecycleStatus
import javax.inject.Singleton

@Singleton
class ApplicationHealthSource : HealthSource {
  override val healthy: Boolean
    get() = ApplicationLifecycleManager.getStatus() == LifecycleStatus.STARTED
  override val sourceName: String
    get() = "Application Overall Health"
}
