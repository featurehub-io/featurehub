package io.featurehub.mr.dacha2

import io.featurehub.dacha2.api.Dacha2Service
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.mr.dacha2.rest.Dacha2Resource
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class Dacha2Feature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object: AbstractBinder() {
      override fun configure() {
        // we bind the resource this way so it exposes two interfaces - then party-server will
        // connect directly here
        bind(Dacha2Resource::class.java)
          .to(Dacha2Service::class.java)
          .to(Dacha2ServiceClient::class.java).`in`(
          Singleton::class.java)
      }

    })

    return true
  }

  companion object {
    /*
     * Only if the dacha2 API keys exist can we mount this API on the public interface
     */
    fun dacha2ApiKeysExist() : Boolean {
      return FallbackPropertyConfig.getConfig("mr.dacha2.api-keys") != null
    }
  }
}
