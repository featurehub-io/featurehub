package io.featurehub.dacha

import io.featurehub.dacha.resource.DachaApiKeyResource
import io.featurehub.dacha.resource.DachaEnvironmentResource
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class DachaFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    arrayOf(
      DachaApiKeyResource::class.java,
      DachaEnvironmentResource::class.java).forEach { clazz -> context.register(clazz) }

    return true
  }
}
