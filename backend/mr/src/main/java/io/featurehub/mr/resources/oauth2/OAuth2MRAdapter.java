package io.featurehub.mr.resources.oauth2;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PersonApi;
import io.featurehub.db.services.InternalOAuthPersonCreation;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.model.Person;
import io.featurehub.web.security.oauth.SSOCompletionListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import static jakarta.ws.rs.core.Cookie.DEFAULT_VERSION;
import static jakarta.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

@Singleton
public class OAuth2MRAdapter implements SSOCompletionListener {
  private static final Logger log = LoggerFactory.getLogger(OAuth2MRAdapter.class);

  protected final PersonApi personApi;
  private final AuthenticationApi authenticationApi;
  private final AuthenticationRepository authRepository;
  private final OrganizationApi organizationApi;
  private final InternalOAuthPersonCreation internalOAuthPersonCreation;

  @ConfigKey("oauth2.cookie.domain")
  String cookieDomain = "";

  @ConfigKey("oauth2.cookie.https-only")
  Boolean cookieSecure = Boolean.FALSE;

  @Inject
  public OAuth2MRAdapter(
      PersonApi personApi,
      AuthenticationApi authenticationApi,
      AuthenticationRepository authRepository,
      OrganizationApi organizationApi,
      InternalOAuthPersonCreation internalOAuthPersonCreation) {
    this.personApi = personApi;
    this.authenticationApi = authenticationApi;
    this.authRepository = authRepository;
    this.organizationApi = organizationApi;
    this.internalOAuthPersonCreation = internalOAuthPersonCreation;

    DeclaredConfigResolver.resolve(this);
  }

  @NotNull
  @Override
  public Response successfulCompletion(
      @Nullable String email,
      @Nullable String username,
      boolean userMustBeCreatedFirst,
      @Nullable String failureUrl,
      @Nullable String successUrl,
      @NotNull String provider) {
    if (email == null || username == null) {
      log.warn("User tried to login via SSO with no email `` or username ``", email, username);

      return Response.status(302).location(URI.create(failureUrl)).build();
    }
    // discover if they are a user and if not, add them
    Person p = personApi.get(email, Opts.empty());

    if (p != null && p.getWhenArchived() != null) {
      log.warn("User {} attempted to login and have been deleted.", email);
      return Response.status(302).location(URI.create(failureUrl)).build();
    }

    if (p == null) {
      // if the user must be created in the database before they are allowed to sign in, redirect to
      // failure.
      if (userMustBeCreatedFirst) {
        log.warn(
            "User {} attempted to login and they aren't in the database and they need to be.",
            email);
        return Response.status(302).location(URI.create(failureUrl)).build();
      }

      p = internalOAuthPersonCreation.createUser(email, username);
    } else {
      p.setName(username);

      try {
        p.setGroups(null); // don't update groups.
        // this is our single write, all in one transaction so we should be good for HA
        personApi.update(p.getId().getId(), p, Opts.empty(), p.getId().getId());
      } catch (OptimisticLockingException ignored) {
      }

      authenticationApi.updateLastAuthenticated(p.getId().getId());
    }

    // store user in session with bearer token
    String token = authRepository.put(p);

    URI uri = URI.create(successUrl);
    // add cookie
    return Response.status(Response.Status.FOUND)
        .cookie(
            new NewCookie(
                "bearer-token",
                token,
                "/",
                cookieDomain.isEmpty() ? null : cookieDomain,
                DEFAULT_VERSION,
                null,
                DEFAULT_MAX_AGE,
                null,
                cookieSecure,
                false))
        .location(uri)
        .build();
  }

  @Override
  public boolean initialAppSetupComplete() {
    return organizationApi.get() != null;
  }
}
