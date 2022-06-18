package io.featurehub.web.security.oauth

import jakarta.ws.rs.NotFoundException

// ensures we have at least one of these as injection requires it
class BlankProviderCollection : SSOProviderCollection {
  override val providers: Collection<AuthProviderInfo>
    get() = emptyList()

  override fun requestRedirectUrl(provider: String): String {
    // if we get picked there is something seriously wrong
    throw NotFoundException()
  }
}
