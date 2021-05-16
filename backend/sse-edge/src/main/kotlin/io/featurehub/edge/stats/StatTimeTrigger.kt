package io.featurehub.edge.stats

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import java.util.*

class StatTimeTrigger(val squashAndPublisher: StatsSquashAndPublisher) {
  // how often will we clear the data out?
  @ConfigKey("edge.stats.publish-interval-ms")
  var publishBundleInterval: Long = 0
  init {
    DeclaredConfigResolver.resolve(this)

    startTimer()
  }

  protected fun startTimer() {
    if (publishBundleInterval > 0) {
      val secondTimer = Timer("countdown-to-publish-stats")
      secondTimer.scheduleAtFixedRate(object : TimerTask() {
        override fun run() {
          squashAndPublisher.squashAndPublish()
        }
      }, 0, publishBundleInterval)
    }
  }

}
