package io.featurehub.mr.auth;

import io.featurehub.mr.model.Person;
import org.apache.commons.lang3.RandomStringUtils;

import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class InMemoryAuthRepository implements AuthenticationRepository {
  Map<String, SessionToken> tokens = new ConcurrentHashMap<>();

  @Override
  public SessionToken get(String sessionToken) {
    return tokens.get(sessionToken);
  }

  @Override
  public String put(Person person) {
    String token = RandomStringUtils.randomAlphanumeric(36);
    SessionToken sessionToken = new SessionToken.Builder().lastSeen(LocalDateTime.now()).person(person).sessionToken(token).build();
    tokens.put(token, sessionToken);
    return token;
  }

  @Override
  public void invalidate(SecurityContext context) {
    AuthHolder holder = ((AuthHolder)context.getUserPrincipal());

    // take it out of the invalidate mapping
    String bearerToken = holder.getSessionToken().sessionToken;

    if (bearerToken != null) {
      tokens.remove(bearerToken);
    }
  }
}
