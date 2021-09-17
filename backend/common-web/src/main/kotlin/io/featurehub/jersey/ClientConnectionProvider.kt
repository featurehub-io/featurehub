package io.featurehub.jersey

import cd.connect.jersey.common.LoggingConfiguration
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.lifecycle.ClientTelemetryFeature
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.ext.ContextResolver
import jakarta.ws.rs.ext.Provider

@Provider
class ClientConnectionProvider: ContextResolver<Client> {
  override fun getContext(type: Class<*>?): Client {
    return ClientBuilder.newClient()
      .register(ClientTelemetryFeature::class.java)
      .register(CommonConfiguration::class.java)
      .register(LoggingConfiguration::class.java)
  }

}
