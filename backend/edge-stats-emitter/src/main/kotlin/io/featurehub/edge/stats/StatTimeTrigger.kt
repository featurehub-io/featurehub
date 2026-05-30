package io.featurehub.edge.stats

import io.featurehub.lifecycle.LifecyclePriority
import io.featurehub.lifecycle.LifecycleShutdown
import io.featurehub.lifecycle.LifecycleStarted
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

@LifecyclePriority(priority = 10)
class StatTimeTrigger @Inject constructor(private val statCollector: StatCollector,
                                          private val orchestrator: StatsOrchestrator): LifecycleShutdown, LifecycleStarted {
  private val log: Logger = LoggerFactory.getLogger(StatTimeTrigger::class.java)

  // how often will we clear the data out?
  var publishBundleInterval: Long = FallbackPropertyConfig.getConfig("edge.stats.publish-interval-ms", "0").toLong()

  private var timer: Timer? = null

  override fun started() {
    if (publishBundleInterval > 0) {
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
