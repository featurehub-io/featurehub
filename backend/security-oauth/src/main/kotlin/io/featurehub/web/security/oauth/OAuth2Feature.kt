package io.featurehub.web.security.oauth

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.common.LoggingConfiguration
import io.featurehub.jersey.config.CommonConfiguration
import io.featurehub.lifecycle.ClientTelemetryFeature
import io.featurehub.web.security.oauth.providers.*
import io.featurehub.web.security.oauth.providers.GithubProvider.Companion.PROVIDER_NAME
import jakarta.inject.Singleton
import jakarta.ws.rs.client.Client
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.slf4j.LoggerFactory
import java.util.function.Consumer

class OAuth2Feature : Feature {
  // a comma separated list of valid providers
  @ConfigKey("oauth2.providers")
  protected var validProviderSources: List<String> = ArrayList()

  override fun configure(context: FeatureContext): Boolean {
    if (!validProviderSources.isEmpty()) {
      val providers: MutableList<Class<out OAuth2Provider>> = ArrayList()
      if (validProviderSources.contains(GoogleProvider.Companion.PROVIDER_NAME)) {
        providers.add(GoogleProvider::class.java)
      }
      if (validProviderSources.contains(AzureProvider.Companion.PROVIDER_NAME)) {
        providers.add(AzureProvider::class.java)
      }
      if (validProviderSources.contains(KeycloakProvider.Companion.PROVIDER_NAME)) {
        providers.add(KeycloakProvider::class.java)
      }
      if (validProviderSources.contains(PROVIDER_NAME)) {
        providers.add(GithubProvider::class.java)
      }
      if (providers.isEmpty()) {
        throw RuntimeException("oauth2.providers list is not empty and contains unsupported oauth2 providers.")
      }
      context.register(OauthResource::class.java)
      context.register(object : AbstractBinder() {
        override fun configure() {
          // bind all the providers
          providers.forEach(Consumer { p: Class<out OAuth2Provider>? ->
            bind(p).to(
              OAuth2Provider::class.java
            ).`in`(Singleton::class.java)
          })

          // the class that allows discovery of the providers
          bind(OAuth2ProviderManager::class.java).to(OAuth2ProviderDiscovery::class.java).to(
            AuthProvider::class.java
          ).`in`(Singleton::class.java)

          // now the outbound http request to validate authorization flow
          bind(OAuth2JerseyClient::class.java).to(OAuth2Client::class.java).`in`(Singleton::class.java)
        }
      })
    } else {
      log.info("No oauth2 providers in config, skipping oauth2.")
    }
    return true
  }

  companion object {
    private val log = LoggerFactory.getLogger(OAuth2Feature::class.java)
  }

  init {
    DeclaredConfigResolver.resolve(this)

    // ensure we don't have any spaces in our config
    validProviderSources = validProviderSources.map { it.trim() }.filter { it.isNotEmpty() }
  }
}
