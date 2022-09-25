package io.featurehub.mr

import io.featurehub.web.security.oauth.AuthProviderCollection
import io.featurehub.web.security.oauth.AuthProviders
import io.featurehub.web.security.oauth.NoAuthProviders
import io.featurehub.web.security.oauth.OAuth2Feature
import io.featurehub.web.security.saml.SamlEnvironmentalFeature
import jakarta.inject.Singleton
import jakarta.ws.rs.core.Feature
import jakarta.ws.rs.core.FeatureContext
import org.glassfish.jersey.internal.inject.AbstractBinder

class AuthProvidersFeature : Feature {
  override fun configure(context: FeatureContext): Boolean {
    context.register(object : AbstractBinder() {
      override fun configure() {
        if (OAuth2Feature.oauth2ProvidersExist() || SamlEnvironmentalFeature.samlProvidersExist()) {
          bind(AuthProviders::class.java).to(AuthProviderCollection::class.java).`in`(Singleton::class.java)
        } else {
          bind(NoAuthProviders::class.java).to(AuthProviderCollection::class.java).`in`(Singleton::class.java)
        }
      }
    })

    return true
  }
}
