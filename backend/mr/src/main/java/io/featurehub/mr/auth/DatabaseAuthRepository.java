package io.featurehub.mr.auth;

import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.DBLoginSession;
import io.featurehub.mr.model.Person;
import org.apache.commons.lang3.RandomStringUtils;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDateTime;

public class DatabaseAuthRepository implements AuthenticationRepository {
  private final AuthenticationApi authenticationApi;

  @Inject
  public DatabaseAuthRepository(AuthenticationApi authenticationApi) {
    this.authenticationApi = authenticationApi;
  }

  @Override
  public SessionToken get(String sessionToken) {
    final DBLoginSession session = authenticationApi.findSession(sessionToken);

    if (session != null) {
      return new SessionToken.Builder()
          .lastSeen(session.getLastSeen())
          .person(session.getPerson())
          .sessionToken(sessionToken)
          .build();
    }

    return null;
  }

  @Override
  public String put(Person person) {
    String token = RandomStringUtils.randomAlphanumeric(36);

    authenticationApi.createSession(new DBLoginSession(person, token, LocalDateTime.now()));

    return token;
  }

  @Override
  public void invalidate(SecurityContext context) {
    AuthHolder holder = ((AuthHolder)context.getUserPrincipal());

    authenticationApi.invalidateSession(holder.getSessionToken().sessionToken);
  }
}
