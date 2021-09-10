package io.featurehub.utils

import java.time.Instant

class CurrentTime: CurrentTimeSource {
  override fun getCurrentTimeInSeconds(): Long {
    return (System.currentTimeMillis() / 1000L)
  }

  override fun getInstantCurrentTime(): Instant {
    return Instant.now()
  }
}
