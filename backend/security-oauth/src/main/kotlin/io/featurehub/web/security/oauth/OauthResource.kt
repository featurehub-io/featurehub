package io.featurehub.web.security.oauth

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.web.security.oauth.providers.OAuth2ProviderDiscovery
import org.slf4j.LoggerFactory
import java.net.URI
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response
import kotlin.String

/*

https://YOUR_DOMAIN/authorize?
    response_type=code&
    client_id=YOUR_CLIENT_ID&
    redirect_uri=https://YOUR_APP/oauth/token&
    scope=SCOPE&
    state=STATE
 */
@Path("/oauth")
class OauthResource @Inject constructor(
    protected val oAuth2Client: OAuth2Client,
    protected val discovery: OAuth2ProviderDiscovery,
    protected val oAuthAdapter: OAuthAdapter
) {
    // where we redirect the user on successful login (with cookie for code)
    @ConfigKey("oauth2.adminUiUrlSuccess")
    protected var successUrl: String? = null

    @ConfigKey("oauth2.adminUiUrlFailure")
    protected var failureUrl: String? = null

    @ConfigKey("auth.userMustBeCreatedFirst")
    protected var userMustBeCreatedFirst: Boolean? = false

    @Path("/auth")
    @GET
    fun token(
        @QueryParam("code") code: String?,
        @QueryParam("state") state: String,
        @QueryParam("error") error: String?
    ): Response? {
        if (error != null) {
            return Response.ok().location(URI.create(failureUrl)).build()
        }

        // not initialized!
        if (!oAuthAdapter.organisationCreationRequired()) {
            return Response.ok().location(URI.create(failureUrl)).build()
        }

        // decode the ProviderUser
        val providerFromState = discovery.getProviderFromState(state)
            ?: return Response.ok().location(URI.create(failureUrl)).build()
        val authed = oAuth2Client.requestAccess(code, providerFromState)
            ?: return Response.ok().location(URI.create(failureUrl)).build()
        log.info("auth was {}", authed)
        val providerUser = providerFromState.discoverProviderUser(authed)
            ?: return Response.ok().location(URI.create(failureUrl)).build()
        return oAuthAdapter.successfulCompletion(
            providerUser.email,
            providerUser.name,
            userMustBeCreatedFirst!!,
            failureUrl,
            successUrl
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(OauthResource::class.java)
    }

    init {
        DeclaredConfigResolver.resolve(this)
    }
}
