package io.featurehub.mr.auth;

import io.featurehub.mr.model.Person;

import jakarta.ws.rs.core.SecurityContext;

public interface AuthenticationRepository {
  SessionToken get(String sessionToken);
  String put(Person person);
  void invalidate(SecurityContext securityContext);
}
