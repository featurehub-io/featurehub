package io.featurehub.web.security.oauth

import io.featurehub.web.security.oauth.providers.SSOProviderCustomisation
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider

interface AuthProviderInfo {
  /**
   * the unique code for this provider
   */
  val code: String

  /**
   * true only if you want this url shown on the login page, otherwise not. It can respond to a
   * request for a login, but will not show in the list.
   */
  val exposeOnLoginPage: Boolean

  /**
   * any login page customisation. If this is null, it is assumed the login page knows how to deal with the
   * "code" (e.g. google, microsoft, github)
   */
  val icon: SSOProviderCustomisation?
}

// this is an opaque pairing of the auth provider info and the method to get a redirect url
// this may need to be revisited when supporting SAML
interface AuthProviderSource {
  val authInfo: AuthProviderInfo
  val redirectUrl: String?
  val code: String
}

// oauth and blank - these have a number of providers under them. saml would also be under this.
interface SSOProviderCollection {
  val providers: Collection<AuthProviderInfo>
  fun requestRedirectUrl(provider: String): String?
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

class AuthProviders @Inject constructor(ssoProvidersCollection: IterableProvider<SSOProviderCollection>
) : AuthProviderCollection {
  inner class InternalAuthProviderSource(override val authInfo: AuthProviderInfo, private val SSOProviderCollection: SSOProviderCollection) : AuthProviderSource {
    override val redirectUrl: String?
      get() = SSOProviderCollection.requestRedirectUrl(code)

    override val code: String
      get() = authInfo.code
  }

  private val flattenedProviderList: MutableList<AuthProviderSource> = mutableListOf()

  init {
    ssoProvidersCollection.forEach { ap ->
      ap.providers.forEach { prov ->
        flattenedProviderList.add(InternalAuthProviderSource(prov, ap))
      }
    }

    flattenedProviderList.sortBy { it.code }
  }

  override val providers: List<AuthProviderSource>
    get() = flattenedProviderList
  override val codes: List<String>
    get() = providers.map { p -> p.code }

  override fun find(code: String): AuthProviderSource? {
    return providers.find { it.code == code }
  }
}
