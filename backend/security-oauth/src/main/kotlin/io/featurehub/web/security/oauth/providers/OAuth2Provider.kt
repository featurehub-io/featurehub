package io.featurehub.web.security.oauth.providers

import io.featurehub.web.security.oauth.AuthClientResult

interface OAuth2Provider {
  fun discoverProviderUser(authed: AuthClientResult): ProviderUser?
  fun providerName(): String
  fun requestTokenUrl(): String
  fun requestAuthorizationUrl(): String
  val clientId: String?
  val clientSecret: String?
}
