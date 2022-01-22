package io.featurehub.mr.auth;

import io.featurehub.db.api.DBLoginSession;
import io.featurehub.db.api.SessionApi;
import io.featurehub.mr.model.Person;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

public class DatabaseAuthRepository implements AuthenticationRepository {
  private final SessionApi sessionApi;

  @Inject
  public DatabaseAuthRepository(SessionApi sessionApi) {
    this.sessionApi = sessionApi;
  }

  @Override
  public SessionToken get(String sessionToken) {
    final DBLoginSession session = sessionApi.findSession(sessionToken);

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

    sessionApi.createSession(new DBLoginSession(person, token, Instant.now()));

    return token;
  }

  @Override
  public void invalidate(SecurityContext context) {
    AuthHolder holder = ((AuthHolder)context.getUserPrincipal());

    sessionApi.invalidateSession(holder.getSessionToken().sessionToken);
  }
}
