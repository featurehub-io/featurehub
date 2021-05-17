package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

class StatTimeTrigger @Inject constructor(private val statCollector: StatCollector,
                                          private val orchestrator: StatsOrchestrator) {
  private val log: Logger = LoggerFactory.getLogger(StatTimeTrigger::class.java)

  // how often will we clear the data out?
  @ConfigKey("edge.stats.publish-interval-ms")
  var publishBundleInterval: Long? = 0

  init {
    DeclaredConfigResolver.resolve(this)

    startTimer()
  }

  protected fun startTimer() {
    if (publishBundleInterval!! > 0) {
      log.info("stats: publishing every {}ms", publishBundleInterval)

      val secondTimer = Timer("countdown-to-publish-stats")
      secondTimer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
          orchestrator.squashAndPublish(statCollector.ejectData())
        }
      }, 0, publishBundleInterval!!)
    }
  }

}
