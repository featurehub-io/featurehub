package io.featurehub.mr.events.common

import io.featurehub.utils.FallbackPropertyConfig

class Dacha2Config {
  companion object {
    fun isDacha2Enabled(): Boolean =
      FallbackPropertyConfig.getConfig("dacha2.enabled") == "true"
  }
}
