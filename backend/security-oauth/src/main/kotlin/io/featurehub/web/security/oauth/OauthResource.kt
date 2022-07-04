package io.featurehub.web.security.oauth

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import cd.connect.jersey.prometheus.Prometheus
import io.featurehub.web.security.oauth.providers.OAuth2ProviderDiscovery
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.net.URI

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
  protected val SSOCompletionListener: SSOCompletionListener
) {
  // where we redirect the user on successful login (with cookie for code)
  @ConfigKey("oauth2.adminUiUrlSuccess")
  protected var successUrl: String? = null

  @ConfigKey("oauth2.adminUiUrlFailure")
  protected var failureUrl: String? = null

  @ConfigKey("auth.userMustBeCreatedFirst")
  protected var userMustBeCreatedFirst: Boolean? = false

  @Path("/auth")
  @Prometheus(name = "oauth2_token", help = "OAuth2 token API")
  @GET
  fun token(
    @QueryParam("code") code: String?,
    @QueryParam("state") state: String,
    @QueryParam("error") error: String?
  ): Response? {
    if (error != null) {
      return Response.status(302).location(URI.create(failureUrl!!)).build()
    }

    // not initialized!
    if (!SSOCompletionListener.initialAppSetupComplete()) {
      return Response.status(302).location(URI.create(failureUrl!!)).build()
    }

    // decode the ProviderUser
    val providerFromState = discovery.getProviderFromState(state)
      ?: return Response.status(302).location(URI.create(failureUrl!!)).build()
    val authed = oAuth2Client.requestAccess(code, providerFromState)
      ?: return Response.status(302).location(URI.create(failureUrl!!)).build()
    val providerUser = providerFromState.discoverProviderUser(authed)
      ?: return Response.status(302).location(URI.create(failureUrl!!)).build()
    return SSOCompletionListener.successfulCompletion(
        providerUser.email,
        providerUser.name,
        userMustBeCreatedFirst!!,
        failureUrl,
        successUrl,
        providerFromState.providerName()
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(OauthResource::class.java)
  }

  init {
    DeclaredConfigResolver.resolve(this)
  }
}
