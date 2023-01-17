package io.featurehub.enricher

import io.featurehub.utils.FallbackPropertyConfig

class EnricherConfig {
  companion object {
    // we use a function for testability
    fun enabled() = FallbackPropertyConfig.getConfig("enricher.enabled")?.lowercase() != "false"
  }
}
