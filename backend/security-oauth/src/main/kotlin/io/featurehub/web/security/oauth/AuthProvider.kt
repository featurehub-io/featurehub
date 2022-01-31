package io.featurehub.web.security.oauth

import io.featurehub.web.security.oauth.providers.OAuth2ProviderCustomisation
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider

interface AuthProviderInfo {
  val code: String
  val icon: OAuth2ProviderCustomisation?
}

// this is an opaque pairing of the auth provider info and the method to get a redirect url
// this may need to be revisited when supporting SAML
interface AuthProviderSource {
  val authInfo: AuthProviderInfo
  val redirectUrl: String
  val code: String
}

// oauth and blank - these have a number of providers under them. saml would also be under this.
interface AuthProvider {
  val providers: Collection<AuthProviderInfo>
  fun requestRedirectUrl(provider: String): String
}

interface AuthProviderCollection {
  val providers: List<AuthProviderSource>
  val codes: List<String>
  fun find(code: String): AuthProviderSource?
}

class NoAuthProviders : AuthProviderCollection {
  override val providers: List<AuthProviderSource>
    get() = listOf()

  override val codes: List<String>
    get() = listOf()

  override fun find(code: String): AuthProviderSource? {
   return null
  }
}

class AuthProviders @Inject constructor(authProviders: IterableProvider<AuthProvider>
) : AuthProviderCollection {
  inner class InternalAuthProviderSource(override val authInfo: AuthProviderInfo, private val authProvider: AuthProvider) : AuthProviderSource {
    override val redirectUrl: String
      get() = authProvider.requestRedirectUrl(code)

    override val code: String
      get() = authInfo.code
  }

  private val flattenedProviderList: MutableList<AuthProviderSource> = mutableListOf()

  init {
    authProviders.forEach { ap ->
      ap.providers.forEach { prov ->
        flattenedProviderList.add(InternalAuthProviderSource(prov, ap))
      }
    }
  }

  override val providers: List<AuthProviderSource>
    get() = flattenedProviderList
  override val codes: List<String>
    get() = providers.map { p -> p.code }

  override fun find(code: String): AuthProviderSource? {
    return providers.find { it.code == code }
  }
}
