package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

class StatTimeTrigger @Inject constructor(val squashAndPublisher: StatsSquashAndPublisher) {
  private val log: Logger = LoggerFactory.getLogger(StatTimeTrigger::class.java)

  // how often will we clear the data out?
  @ConfigKey("edge.stats.publish-interval-ms")
  var publishBundleInterval: java.lang.Long = java.lang.Long(0)

  init {
    DeclaredConfigResolver.resolve(this)

    startTimer()
  }

  protected fun startTimer() {
    if (publishBundleInterval > 0) {
      log.info("stats: publishing every {}ms", publishBundleInterval)

      val secondTimer = Timer("countdown-to-publish-stats")
      secondTimer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
          squashAndPublisher.squashAndPublish()
        }
      }, 0, publishBundleInterval.toLong())
    }
  }

}
