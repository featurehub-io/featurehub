package io.featurehub.web.security.oauth

interface AuthProvider {
  val providers: Collection<String>
  fun requestRedirectUrl(provider: String): String
}
