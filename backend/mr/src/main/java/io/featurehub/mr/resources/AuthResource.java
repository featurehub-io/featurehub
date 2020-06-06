package io.featurehub.mr.resources;

import io.featurehub.db.api.AuthenticationApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.mr.api.AuthServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.auth.AuthenticationRepository;
import io.featurehub.mr.model.PasswordReset;
import io.featurehub.mr.model.PasswordUpdate;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonRegistrationDetails;
import io.featurehub.mr.model.TokenizedPerson;
import io.featurehub.mr.model.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.SecurityContext;

public class AuthResource implements AuthServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(AuthResource.class);
  private final AuthenticationApi authenticationApi;
  private final AuthManagerService authManager;
  private final PersonApi personApi;
  private final AuthenticationRepository authRepository;

  @Inject
  public AuthResource(AuthenticationApi authenticationApi, AuthManagerService authManager, PersonApi personApi, AuthenticationRepository authRepository) {
    this.authenticationApi = authenticationApi;
    this.authManager = authManager;
    this.personApi = personApi;
    this.authRepository = authRepository;
  }

  @Override
  public Person changePassword(String id, PasswordUpdate passwordUpdate, SecurityContext securityContext) {
    Person personByToken = authManager.from(securityContext);

    // yourself or a superuser can change your password. This allows a superuser to change the password immediately
    // after reset without having to go to any further trouble.
    if (personByToken.getId().getId().equals(id) || authManager.isOrgAdmin(personByToken)) {
      Person newPerson = authenticationApi.changePassword(id, passwordUpdate.getOldPassword(), passwordUpdate.getNewPassword());

      if (newPerson == null) {
        throw new BadRequestException("Old password does not match.");
      }

      return newPerson;
    }

    throw new ForbiddenException();
  }

  @Override
  public TokenizedPerson login(UserCredentials userCredentials) {
    Person login = authenticationApi.login(userCredentials.getEmail(), userCredentials.getPassword());

    if (login == null) {
      throw new NotFoundException();
    }

    return new TokenizedPerson().accessToken(authRepository.put(login)).person(login);
  }

  @Override
  public void logout(SecurityContext securityContext) {
    authRepository.invalidate(securityContext);
  }

  @Override
  public Person personByToken(String token) {
    Person personByToken = authenticationApi.getPersonByToken(token);

    if (personByToken == null) {
      throw new NotFoundException("No person by that token");
    }

    return personByToken;
  }

  @Override
  public TokenizedPerson registerPerson(PersonRegistrationDetails personRegistrationDetails) {

    //check user found by token and token hasn't expired

    Person person = personApi.getByToken(personRegistrationDetails.getRegistrationToken(), Opts.opts(FillOpts.Groups));

    if (person == null) {
      throw new NotFoundException("Person already registered using token");
    }

    if (!person.getEmail().toLowerCase().equals(personRegistrationDetails.getEmail().toLowerCase())) {
      log.info("db user email `{}` does not match passed email `{}`", person.getEmail(), personRegistrationDetails.getEmail());
      //registration token doesn't belong to provided email
      throw new BadRequestException();
    }

    if (!personRegistrationDetails.getPassword().equals(personRegistrationDetails.getConfirmPassword())) {
      //passwords don't match
      throw new BadRequestException();
    }

    Person newPerson = authenticationApi.register(personRegistrationDetails.getName(),
      personRegistrationDetails.getEmail(),
      personRegistrationDetails.getPassword());

    if (newPerson == null) {
      throw new NotFoundException("Cannot find person to register");
    }

    return new TokenizedPerson().accessToken(authRepository.put(person)).person(person);
  }

  @Override
  public TokenizedPerson replaceTempPassword(String id, PasswordReset passwordReset, SecurityContext context) {
    Person person = authManager.from(context);

    if (Boolean.TRUE.equals(person.getPasswordRequiresReset())) {
      if (person.getId().getId().equals(id)) { // its me
        Person newPerson = authenticationApi.replaceTemporaryPassword(id, passwordReset.getPassword());
        authRepository.invalidate(context);
        return new TokenizedPerson().accessToken(authRepository.put(newPerson)).person(newPerson);
      }
    }

    throw new ForbiddenException();
  }

  @Override
  public Person resetPassword(String id, PasswordReset passwordReset, SecurityContext context) {
    if (authManager.isAnyAdmin(authManager.from(context))) {
      Person person = authenticationApi.resetPassword(id, passwordReset.getPassword(),
        authManager.from(context).getId().getId(),
        Boolean.TRUE.equals(passwordReset.getReactivate()));

      if (person == null) {
        throw new NotFoundException();
      }

      return person;
    }

    throw new ForbiddenException();
  }
}
