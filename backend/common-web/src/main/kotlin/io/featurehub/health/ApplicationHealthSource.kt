package io.featurehub.health

import io.featurehub.lifecycle.ApplicationStarted
import io.featurehub.lifecycle.LifecycleStatus
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ApplicationHealthSource @Inject constructor(private val app: ApplicationStarted) : HealthSource {
  override val healthy: Boolean
    get() = app.status == LifecycleStatus.STARTED
  override val sourceName: String
    get() = "Application Overall Health"
}
