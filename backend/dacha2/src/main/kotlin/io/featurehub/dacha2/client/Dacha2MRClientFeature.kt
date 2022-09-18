package io.featurehub.dacha2.client

import cd.connect.openapi.support.ApiClient
import io.featurehub.dacha2.api.Dacha2Service
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.dacha2.api.impl.Dacha2ServiceServiceImpl
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.internal.inject.AbstractBinder

class Dacha2MRClientFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(Dacha2Client::class.java).to(Dacha2ServiceClient::class.java).`in`(Immediate::class.java)
      }
    })

    return true
  }
}

class Dacha2Client @Inject constructor(client: Client) : Dacha2ServiceServiceImpl(ApiClient(client,
  FallbackPropertyConfig.getMandatoryConfig("management-repository.url")
))
