package io.featurehub.utils

import java.time.Instant

interface CurrentTimeSource {
  fun getCurrentTimeInSeconds(): Long
  fun getInstantCurrentTime(): Instant
}
