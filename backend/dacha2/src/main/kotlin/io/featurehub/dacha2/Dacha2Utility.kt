package io.featurehub.dacha2

import io.featurehub.utils.FallbackPropertyConfig

class Dacha2Utility {
  companion object {
    val usingGuavaCache = FallbackPropertyConfig.getConfig("dacha2.use-old-cache", "false") == "true"
  }
}
