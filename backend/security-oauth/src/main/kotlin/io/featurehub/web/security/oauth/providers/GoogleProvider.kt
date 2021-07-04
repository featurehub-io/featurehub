package io.featurehub.web.security.oauth.providers

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.web.security.oauth.AuthClientResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GoogleProvider : OAuth2Provider {
    @ConfigKey("oauth2.providers.google.secret")
    override var clientSecret: String? = null
        protected set

    @ConfigKey("oauth2.providers.google.id")
    override var clientId: String? = null
        protected set

    @ConfigKey("oauth2.redirectUrl")
    protected var redirectUrl: String? = null
    private val actualAuthUrl: String
    private val tokenUrl: String
    override fun discoverProviderUser(authed: AuthClientResult): ProviderUser? {
        val idInfo = Jwt.decodeJwt(authed.idToken) ?: return null
        return ProviderUser.Builder().email(idInfo["email"])
            .name(idInfo["given_name"].toString() + " " + idInfo["family_name"]).build()
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
        const val PROVIDER_NAME = "oauth2-google"
    }

    init {
        DeclaredConfigResolver.resolve(this)
        actualAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?&scope=profile%20email&access_type=online" +
                "&include_granted_scopes=true&response_type=code&client_id=" + clientId + "&redirect_uri=" +
                URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)
        tokenUrl = "https://oauth2.googleapis.com/token"
    }
}
