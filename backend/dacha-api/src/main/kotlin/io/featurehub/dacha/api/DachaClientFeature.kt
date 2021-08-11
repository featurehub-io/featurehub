package io.featurehub.dacha.api

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.LoggingConfiguration
import cd.connect.openapi.support.ApiClient
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.dacha.api.DachaEnvironmentService
import io.featurehub.dacha.api.impl.DachaApiKeyServiceServiceImpl
import io.featurehub.dacha.api.impl.DachaEnvironmentServiceServiceImpl
import io.featurehub.jersey.config.CommonConfiguration
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.hk2.utilities.binding.AbstractBinder

class DachaClientFeature : Feature {
  @ConfigKey("dacha.url")
  var dachaUrl = "http://localhost:8094"

  init {
    DeclaredConfigResolver.resolve(this)
  }

  override fun configure(context: FeatureContext): Boolean {
    val client = ClientBuilder.newClient()
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)

    val apiClient = ApiClient(client, dachaUrl)

    val environmentServiceClient = DachaEnvironmentServiceServiceImpl(apiClient)
    val apiKeyClient = DachaApiKeyServiceServiceImpl(apiClient)

    context.register(object: AbstractBinder() {
      override fun configure() {
        bind(environmentServiceClient).to(DachaEnvironmentService::class.java)
        bind(apiKeyClient).to(DachaApiKeyService::class.java)
      }
    })

    return true
  }

}
