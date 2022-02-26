package io.featurehub.mr.resources.oauth2;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
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
import io.featurehub.mr.utils.PortfolioUtils;
import io.featurehub.web.security.oauth.OAuthAdapter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static jakarta.ws.rs.core.Cookie.DEFAULT_VERSION;
import static jakarta.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Singleton
public class OAuth2MRAdapter implements OAuthAdapter {
  private static final Logger log = LoggerFactory.getLogger(OAuth2MRAdapter.class);

  protected final PersonApi personApi;
  private final AuthenticationApi authenticationApi;
  private final PortfolioApi portfolioApi;
  private final GroupApi groupApi;
  private final AuthenticationRepository authRepository;
  private final PortfolioUtils portfolioUtils;
  private final OrganizationApi organizationApi;

  @ConfigKey("oauth2.cookie.domain")
  String cookieDomain = "";

  @ConfigKey("oauth2.cookie.https-only")
  Boolean cookieSecure = Boolean.FALSE;

  @Inject
  public OAuth2MRAdapter(PersonApi personApi, AuthenticationApi authenticationApi, PortfolioApi portfolioApi,
                         GroupApi groupApi, AuthenticationRepository authRepository, PortfolioUtils portfolioUtils, OrganizationApi organizationApi) {
    this.personApi = personApi;
    this.authenticationApi = authenticationApi;
    this.portfolioApi = portfolioApi;
    this.groupApi = groupApi;
    this.authRepository = authRepository;
    this.portfolioUtils = portfolioUtils;
    this.organizationApi = organizationApi;

    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public Response successfulCompletion(String email, String username, boolean userMustBeCreatedFirst,
                                       String failureUrl, String successUrl) {
    // discover if they are a user and if not, add them
    Person p = personApi.get(email, Opts.empty());

    if (p != null && p.getWhenArchived() != null) {
      log.warn("User {} attempted to login and have been deleted.", email);
      return Response.status(302).location(URI.create(failureUrl)).build();
    }

    if (p == null) {
      // if the user must be created in the database before they are allowed to sign in, redirect to failure.
      if (userMustBeCreatedFirst) {
        log.warn("User {} attempted to login and they aren't in the database and they need to be.", email);
        return Response.status(302).location(URI.create(failureUrl)).build();
      }

      p = createUser(email, username);
    } else {
      p.setName(username);

      try {
        p.setGroups(null); // don't update groups.
        personApi.update(p.getId().getId(), p, Opts.empty(), p.getId().getId());
      } catch (OptimisticLockingException ignored) {
      }

      authenticationApi.updateLastAuthenticated(p.getId().getId());
    }

    // store user in session with bearer token
    String token = authRepository.put(p);

    URI uri = URI.create(successUrl);
    // add cookie
    return Response.status(Response.Status.FOUND).cookie(
      new NewCookie("bearer-token", token, "/",
        cookieDomain.isEmpty() ? null : cookieDomain, DEFAULT_VERSION, null, DEFAULT_MAX_AGE, null, cookieSecure,
        false))
      .location(uri).build();
  }

  private Person createUser(String email, String username) {
    // determine if they were the first user, and if so, complete setup
    boolean firstUser = personApi.noUsersExist();

    // first we create them, this will give them a token and so forth, we are playing with existing functionality
    // here
    try {
      personApi.create(email, username,null);
    } catch (PersonApi.DuplicatePersonException e) {
      log.error("Shouldn't get here, as we check if the person exists before creating them.");
      return null;
    }

    // now "register" them. We can provide a null password OK, it just ignores it, but this removes
    // any registration token required
    Person person = authenticationApi.register(username, email, null, null);

    if (firstUser) {
      Organization organization = organizationApi.get();
      // create the superuser group and add admin to the group -
      Group group = groupApi.createOrgAdminGroup(organization.getId(), "org_admin", person);
      groupApi.addPersonToGroup(group.getId(), person.getId().getId(), Opts.empty());

      // find the only portfolio and update its members to include this one
      final Portfolio portfolio = portfolioApi.findPortfolios(null, SortOrder.ASC,
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

  @Override
  public boolean initialAppSetupComplete() {
    return organizationApi.get() != null;
  }
}
