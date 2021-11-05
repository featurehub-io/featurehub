package io.featurehub.web.security.oauth.providers

import io.featurehub.web.security.oauth.AuthClientResult

interface OAuth2Provider {
    // 3: convert AuthClientResult to an actual user we can use
  fun discoverProviderUser(authed: AuthClientResult): ProviderUser?

    // the name of the provider must match what is configured for exporting to the front end
  fun providerName(): String

    // 2: once the provider has called back with the code, we need to call back to the provider from our SERVER
    // (NOT the web front end) with this token + the secret and get the details of the user. This process will result in a
    // AuthClientResult which gets resolved in discoverProviderUser
  fun requestTokenUrl(): String

    // 1: if a person chooses this provider, then they get redirected to this url - completely away from our app,
    // NOT SPA. When the user has logged in, it will redirect the user back to our web host (NOT our front end)
    // and provide a code. This code is used in requestTokenUrl
  fun requestAuthorizationUrl(): String

  val clientId: String?
  val clientSecret: String?
}
