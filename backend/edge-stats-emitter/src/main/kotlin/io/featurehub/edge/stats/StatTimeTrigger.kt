package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@LifecyclePriority(priority = 10)
class StatTimeTrigger @Inject constructor(private val statCollector: StatCollector,
                                          private val orchestrator: StatsOrchestrator): LifecycleShutdown, LifecycleStarted {
  private val log: Logger = LoggerFactory.getLogger(StatTimeTrigger::class.java)

  // how often will we clear the data out?
  @ConfigKey("edge.stats.publish-interval-ms")
  var publishBundleInterval: Long? = 0

  private var timer: Timer? = null

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun started() {
    if (publishBundleInterval!! > 0) {
      log.info("stats: publishing every {}ms", publishBundleInterval)

      timer = Timer("countdown-to-publish-stats")
      timer!!.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
          orchestrator.squashAndPublish(statCollector.ejectData())
        }
      }, 0, publishBundleInterval!!)
    }
  }

  override fun shutdown() {
    timer?.cancel()
    timer = null
  }
}
