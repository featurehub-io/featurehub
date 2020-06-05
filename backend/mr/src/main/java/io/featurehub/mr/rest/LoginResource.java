package io.featurehub.mr.rest;

import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.mr.api.LoginSecuredService;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.TokenizedPerson;
import io.featurehub.mr.model.UserCredentials;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;

@Singleton
public class LoginResource implements LoginSecuredService {
  private final AuthenticationApi authenticationApi;
  private final AuthenticationRepository authRepository;

  @Inject
  public LoginResource(AuthenticationApi authenticationApi, AuthenticationRepository authRepository) {
    this.authenticationApi = authenticationApi;
    this.authRepository = authRepository;
  }

  @Override
  public TokenizedPerson login(UserCredentials userCredentials) {
    Person login = authenticationApi.login(userCredentials.getEmail(), userCredentials.getPassword());

    if (login == null) {
      throw new NotFoundException();
    }
    
    return new TokenizedPerson().accessToken(authRepository.put(login)).person(login);
  }
}
