package io.featurehub.web.security.oauth.providers

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.web.security.oauth.AuthClientResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// register your app here: https://go.microsoft.com/fwlink/?linkid=2083908
// https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow
class AzureProvider : OAuth2Provider {
    @ConfigKey("oauth2.providers.azure.tenant")
    protected var tenant: String? = null

    @ConfigKey("oauth2.providers.azure.secret")
    override var clientSecret: String? = null
        protected set

    @ConfigKey("oauth2.providers.azure.id")
    override var clientId: String? = null
        protected set

    @ConfigKey("oauth2.redirectUrl")
    protected var redirectUrl: String? = null

    @ConfigKey("oauth2.providers.azure.scopes")
    protected var scopes = "openid email profile"
    private val actualAuthUrl: String
    private val tokenUrl: String

    override fun discoverProviderUser(authed: AuthClientResult): ProviderUser? {
        val idInfo = Jwt.decodeJwt(authed.idToken) ?: return null
        return ProviderUser.Builder().email(idInfo["email"]).name(idInfo["name"]).build()
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
        const val PROVIDER_NAME = "oauth2-azure"
    }

    init {
        DeclaredConfigResolver.resolve(this)
        actualAuthUrl = String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?" +
                    "response_mode=query&scope=%s&include_granted_scopes=true&response_type=code&client_id=%s&redirect_uri=%s",
            tenant,
            URLEncoder.encode(scopes, StandardCharsets.UTF_8),
            clientId,
            URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8)
        )
        tokenUrl = String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenant)
    }
}
