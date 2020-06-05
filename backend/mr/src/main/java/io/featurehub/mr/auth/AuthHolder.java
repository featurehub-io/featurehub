package io.featurehub.mr.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.featurehub.mr.model.Person;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class AuthHolder implements SecurityContext, Principal {
  private final SessionToken sessionToken;

  public AuthHolder(SessionToken sessionToken) {
    this.sessionToken = sessionToken;
  }

  @Override
  @JsonIgnore
  public Principal getUserPrincipal() {
    return this;
  }

  @Override
  @JsonIgnore
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  @JsonIgnore
  public boolean isSecure() {
    return true;
  }

  @Override
  @JsonIgnore
  public String getAuthenticationScheme() {
    return "https";
  }

  @JsonIgnore
  public SessionToken getSessionToken() {
    return sessionToken;
  }

  @Override
  @JsonIgnore
  public String getName() {
    return sessionToken.person.getName();
  }

  @JsonIgnore
  public Person getPerson() {
    return sessionToken.person;
  }
}
