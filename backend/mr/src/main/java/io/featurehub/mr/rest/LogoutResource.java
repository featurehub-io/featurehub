package io.featurehub.mr.rest;

import io.featurehub.mr.api.LogoutSecuredService;
import io.featurehub.mr.auth.AuthenticationRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;

/**
 * @author Richard Vowles - https://plus.google.com/+RichardVowles
 */
@Singleton
public class LogoutResource implements LogoutSecuredService {
  private final AuthenticationRepository authenticationRepository;

  @Inject
  public LogoutResource(AuthenticationRepository authenticationRepository) {
    this.authenticationRepository = authenticationRepository;
  }

  @Override
  public void logout(SecurityContext securityContext) {
    authenticationRepository.invalidate(securityContext);
  }
}
