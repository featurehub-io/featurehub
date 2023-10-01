package io.featurehub.mr.utils

import io.featurehub.utils.FallbackPropertyConfig
import io.featurehub.utils.FallbackPropertyConfig.Companion.getConfig

class ConfigurationUtils {
  companion object {
    val enricherEnabled = "true".equals(
      FallbackPropertyConfig.Companion.getConfig(
        "enricher.enabled", "true"
      ), ignoreCase = true
    )

    val dacha1Enabled = !"false".equals(
      getConfig(
        "dacha1.enabled", "true"
      ), ignoreCase = true
    )

    val webhooksEnabled = "true".equals(
      getConfig(
        "webhooks.features.enabled", "true"
      ), ignoreCase = true
    )

    val messagingEnabled = "true".equals(
      getConfig(
        "webhooks.features.enabled", "true"
      ), ignoreCase = true
    )

    val featureGroupsEnabled = "true".equals(getConfig(
      "feature-groups.enabled", "true"
    ), ignoreCase = true)
  }
}
