package io.featurehub.mr.resources.oauth2;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PersonApi;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.SortOrder;
import io.featurehub.mr.resources.oauth2.providers.OAuth2Provider;
import io.featurehub.mr.resources.oauth2.providers.OAuth2ProviderDiscovery;
import io.featurehub.mr.resources.oauth2.providers.ProviderUser;
import io.featurehub.mr.utils.PortfolioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;

import static javax.ws.rs.core.Cookie.DEFAULT_VERSION;
import static javax.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

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
  protected final PersonApi personApi;
  protected final OAuth2ProviderDiscovery discovery;
  private final AuthenticationRepository authRepository;
  private final AuthenticationApi authenticationApi;
  private final PortfolioApi portfolioApi;
  private final GroupApi groupApi;
  private final PortfolioUtils portfolioUtils;
  private final OrganizationApi organizationApi;


  @Inject
  public OauthResource(OAuth2Client oAuth2Client, PersonApi personApi, OAuth2ProviderDiscovery discovery,
                       AuthenticationRepository authRepository, AuthenticationApi authenticationApi,
                       PortfolioApi portfolioApi, GroupApi groupApi, PortfolioUtils portfolioUtils,
                       OrganizationApi organizationApi) {
    this.oAuth2Client = oAuth2Client;
    this.personApi = personApi;
    this.discovery = discovery;
    this.authRepository = authRepository;
    this.authenticationApi = authenticationApi;
    this.portfolioApi = portfolioApi;
    this.groupApi = groupApi;
    this.portfolioUtils = portfolioUtils;
    this.organizationApi = organizationApi;

    DeclaredConfigResolver.resolve(this);
  }

  @Path("/auth")
  @GET
  public Response token(@QueryParam("code") String code, @QueryParam("state") String state,
                        @QueryParam("error") String error) {
    if (error != null) {
      return Response.ok().location(URI.create(failureUrl)).build();
    }

    // not initialized!
    if (organizationApi.get() == null) {
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

    // discover if they are a user and if not, add them
    Person p = personApi.get(providerUser.getEmail(), Opts.empty());

    if (p == null) {
      // if the user must be created in the database before they are allowed to sign in, redirect to failure.
      if (userMustBeCreatedFirst) {
        log.info("User {} attempted to login and they aren't in the database and they need to be.", providerUser.getEmail());
        return Response.ok().location(URI.create(failureUrl)).build();
      }

      p = createUser(providerUser);
    }

    // store user in session with bearer token
    String token = authRepository.put(p);

    URI uri = URI.create(successUrl);
    // add cookie
    return Response.status(Response.Status.FOUND).cookie(
      new NewCookie("bearer-token", token, "/",
        null, DEFAULT_VERSION, null, DEFAULT_MAX_AGE, null, false, false))
      .location(uri).build();
  }

  private Person createUser(ProviderUser providerUser) {
    // determine if they were the first user, and if so, complete setup
    boolean firstUser = personApi.noUsersExist();

    try {
      personApi.create(providerUser.getEmail(), providerUser.getName(),null);
    } catch (PersonApi.DuplicatePersonException e) {
      log.error("Shouldn't get here, as we check if the person exists before creating them.");
      return null;
    }

    // now register them
    Person person = authenticationApi.register(providerUser.getName(), providerUser.getEmail(),
      null);

    if (firstUser) {
      Organization organization = organizationApi.get();
      // create the superuser group and add admin to the group -
      Group group = groupApi.createOrgAdminGroup(organization.getId(), "org_admin", person);
      groupApi.addPersonToGroup(group.getId(), person.getId().getId(), Opts.empty());

      // find the only portfolio and update its members to include this one
      final Portfolio portfolio = portfolioApi.findPortfolios(null, organization.getId(), SortOrder.ASC,
        Opts.empty(), person).get(0);

      try {
        groupApi.createPortfolioGroup(portfolio.getId(),
          new Group().name(portfolioUtils.formatPortfolioAdminGroupName(portfolio)).admin(true), person);
      } catch (GroupApi.DuplicateGroupException e) {
        log.error("If we have this exception, the site is broken.", e);
      }
    }

    return person;
  }
}


