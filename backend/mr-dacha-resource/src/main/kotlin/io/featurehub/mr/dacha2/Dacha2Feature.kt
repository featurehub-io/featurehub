package io.featurehub.mr.dacha2

import io.featurehub.mr.dacha2.rest.Dacha2Resource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext

class Dacha2Feature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(Dacha2Resource::class.java)

    return true
  }

  companion object {
    /*
     * Only if the dacha2 API keys exist can we mount this API on the pubic interface
     */
    fun dacha2ApiKeysExist() : Boolean {
      return FallbackPropertyConfig.getConfig("mr.dacha2.api-keys") != null
    }
  }
}
