package io.featurehub.web.security.oauth.providers

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.web.security.oauth.AuthClientResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class KeycloakProvider : OAuth2Provider {
  @ConfigKey("oauth2.providers.keycloak.secret")
  override var clientSecret: String? = null
    protected set

  @ConfigKey("oauth2.providers.keycloak.url")
  private var keycloakUrl: String? = null

  @ConfigKey("oauth2.providers.keycloak.realm")
  private var keycloakRealm: String? = null

  @ConfigKey("oauth2.providers.keycloak.id")
  override var clientId: String? = null
    protected set

  @ConfigKey("oauth2.redirectUrl")
  protected var redirectUrl: String? = null

  private val actualAuthUrl: String
  private val tokenUrl: String

  init {
    DeclaredConfigResolver.resolve(this)

    actualAuthUrl =
      "$keycloakUrl/auth/realms/$keycloakRealm/protocol/openid-connect/auth?client_id=" + URLEncoder.encode(
        clientId,
        StandardCharsets.UTF_8
      ) +
        "&response_type=code&scope=profile%20email&redirect_uri=" + URLEncoder.encode(
        redirectUrl,
        StandardCharsets.UTF_8
      )
    tokenUrl = "$keycloakUrl/auth/realms/$keycloakRealm/protocol/openid-connect/token"
  }

  override fun discoverProviderUser(authed: AuthClientResult): ProviderUser? {
    val idInfo = Jwt.decodeJwt(authed.accessToken) ?: return null
    return ProviderUser.Builder().email(idInfo["email"].toString())
      .name(idInfo["name"].toString()).build()
  }

  override fun providerName(): String {
    return PROVIDER_NAME
  }

  override fun requestTokenUrl(): String {
    return tokenUrl
  }

  override fun requestAuthorizationUrl(): String {
    return actualAuthUrl
  }

  companion object {
    const val PROVIDER_NAME = "oauth2-keycloak"
  }
}
