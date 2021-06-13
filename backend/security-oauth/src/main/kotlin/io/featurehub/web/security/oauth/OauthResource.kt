package io.featurehub.web.security.oauth;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.web.security.oauth.providers.OAuth2Provider;
import io.featurehub.web.security.oauth.providers.OAuth2ProviderDiscovery;
import io.featurehub.web.security.oauth.providers.ProviderUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;

/*

https://YOUR_DOMAIN/authorize?
    response_type=code&
    client_id=YOUR_CLIENT_ID&
    redirect_uri=https://YOUR_APP/oauth/token&
    scope=SCOPE&
    state=STATE
 */

@Path("/oauth")
public class OauthResource {
  private static final Logger log = LoggerFactory.getLogger(OauthResource.class);

  // where we redirect the user on successful login (with cookie for code)
  @ConfigKey("oauth2.adminUiUrlSuccess")
  protected String successUrl;

  @ConfigKey("oauth2.adminUiUrlFailure")
  protected String failureUrl;

  @ConfigKey("auth.userMustBeCreatedFirst")
  protected Boolean userMustBeCreatedFirst = Boolean.FALSE;

  protected final OAuth2Client oAuth2Client;
  protected final OAuth2ProviderDiscovery discovery;
  protected final OAuthAdapter oAuthAdapter;

  @Inject
  public OauthResource(
      OAuth2Client oAuth2Client, OAuth2ProviderDiscovery discovery, OAuthAdapter oAuthAdapter) {
    this.oAuth2Client = oAuth2Client;
    this.discovery = discovery;
    this.oAuthAdapter = oAuthAdapter;

    DeclaredConfigResolver.resolve(this);
  }

  @Path("/auth")
  @GET
  public Response token(
      @QueryParam("code") String code,
      @QueryParam("state") String state,
      @QueryParam("error") String error) {
    if (error != null) {
      return Response.ok().location(URI.create(failureUrl)).build();
    }

    // not initialized!
    if (!oAuthAdapter.organisationCreationRequired()) {
      return Response.ok().location(URI.create(failureUrl)).build();
    }

    // decode the ProviderUser
    OAuth2Provider providerFromState = discovery.getProviderFromState(state);

    if (providerFromState == null) {
      return Response.ok().location(URI.create(failureUrl)).build();
    }

    AuthClientResult authed = oAuth2Client.requestAccess(code, providerFromState);

    if (authed == null) {
      return Response.ok().location(URI.create(failureUrl)).build();
    }

    log.info("auth was {}", authed);

    ProviderUser providerUser = providerFromState.discoverProviderUser(authed);

    if (providerUser == null) {
      return Response.ok().location(URI.create(failureUrl)).build();
    }

    return oAuthAdapter.successfulCompletion(
        providerUser.getEmail(),
        providerUser.getName(),
        userMustBeCreatedFirst,
        failureUrl,
        successUrl);
  }
}
