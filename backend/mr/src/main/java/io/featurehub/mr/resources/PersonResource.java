package io.featurehub.mr.resources;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.db.services.Conversions;
import io.featurehub.mr.api.PersonServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.CreatePersonDetails;
import io.featurehub.mr.model.OutstandingRegistration;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RegistrationUrl;
import io.featurehub.mr.model.SearchPersonResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PersonResource implements PersonServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(PersonResource.class);
  private final PersonApi personApi;
  private final GroupApi groupApi;
  private final AuthManagerService authManager;

  @ConfigKey("register.url")
  private String registrationUrl = "http://localhost:register-url?token=%s";

  @Inject
  public PersonResource(PersonApi personApi, GroupApi groupApi, AuthManagerService authManager) {
    this.personApi = personApi;
    this.groupApi = groupApi;
    this.authManager = authManager;

    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public RegistrationUrl createPerson(CreatePersonDetails createPersonDetails, CreatePersonHolder holder,
                                      SecurityContext securityContext) {
    Person currentUser = authManager.from(securityContext);

    if (authManager.isAnyAdmin(currentUser.getId().getId())) {
      //create new user in the db
      try {
        PersonApi.PersonToken person = personApi.create(createPersonDetails.getEmail(),
          createPersonDetails.getName(), currentUser.getId().getId());

        if (person == null || person.id == null) {
          throw new BadRequestException();
        }

        if (createPersonDetails.getGroupIds() != null) {
          //add user to the groups
          Optional.of(createPersonDetails.getGroupIds()).ifPresent(list -> list.forEach(id -> {
            groupApi.addPersonToGroup(id, person.id, Opts.empty());
          }));
        }

        //return registration url
        RegistrationUrl regUrl = new RegistrationUrl();
        // hard code the return value, it will be ignored by the client from now on
        regUrl.setRegistrationUrl(String.format(registrationUrl, person.token));
        regUrl.setToken(person.token);
        return regUrl;
      } catch (PersonApi.DuplicatePersonException e) {
        throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
      }
    }

    throw new ForbiddenException("Not admin");
  }

  private Opts peopleOpts(Boolean includeAcls, Boolean includeGroups) {
    return new Opts()
      .add(FillOpts.Groups, includeGroups)
      .add(FillOpts.Acls, includeAcls);
  }


  private Person getPerson(String id, Boolean includeAcls, Boolean includeGroups, SecurityContext securityContext) {
    Person currentUser = authManager.from(securityContext);


    UUID personId;
    if ("self".equals(id)) {
      personId = currentUser.getId().getId();
      log.debug("User requested their own details: {}", id);
    } else {
      personId = Conversions.checkUuid(id);
    }

    if (currentUser.getId().getId().equals(personId) || authManager.isAnyAdmin(currentUser.getId().getId())) {
      Person person;

      if (personId == null) {
        person = personApi.get(id, peopleOpts(includeAcls, includeGroups));
      } else {
        person = personApi.get(personId, peopleOpts(includeAcls, includeGroups));
      }

      if (person == null) {
        throw new NotFoundException();
      }

      return person;
    }

    throw new ForbiddenException("You are not allowed the details of this person");
  }

  @Override
  public Boolean deletePerson(String id, DeletePersonHolder holder, SecurityContext securityContext) {
    if (authManager.isOrgAdmin(authManager.from(securityContext))) {
      Person p = getPerson(id, false, false, securityContext);

      if (p.getEmail() != null) {
        return personApi.delete(p.getEmail());
      }
    }

    throw new ForbiddenException("No permission");
  }

  @Override
  public SearchPersonResult findPeople(FindPeopleHolder holder, SecurityContext securityContext) {
    Person currentUser = authManager.from(securityContext);

    if (authManager.isAnyAdmin(currentUser.getId().getId())) {

      int start = holder.startAt == null ? 0 : holder.startAt;
      int page = holder.pageSize == null ? 20 : holder.pageSize;

      PersonApi.PersonPagination pp = personApi.search(holder.filter, holder.order, start, page,
        new Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.PersonLastLoggedIn, holder.includeLastLoggedIn));

      return new SearchPersonResult()
        .people(pp.people)
        .outstandingRegistrations(pp.personsWithOutstandingTokens.stream().map(
          pt -> new OutstandingRegistration().id(pt.id).token(pt.token).expired(pp.personIdsWithExpiredTokens.contains(pt.id))
        ).collect(Collectors.toList()))
        .max(pp.max);
    }

    throw new ForbiddenException("Not admin");
  }

  @Override
  public Person getPerson(String id, GetPersonHolder holder, SecurityContext securityContext) {
    return getPerson(id, holder.includeAcls, holder.includeGroups, securityContext);
  }

  @Override
  public Person updatePerson(String id, Person person, UpdatePersonHolder holder, SecurityContext securityContext) {
    Person from = authManager.from(securityContext);

    if (authManager.isAnyAdmin(from)) {
      Person updatedPerson = null;
      try {
        updatedPerson = personApi.update(Conversions.checkUuid(id), person,
          peopleOpts(holder.includeAcls, holder.includeGroups),
          from.getId().getId());
      } catch (OptimisticLockingException e) {
        throw new WebApplicationException(422);
      } catch (IllegalArgumentException iae) {
        throw new BadRequestException();
      }

      if (updatedPerson == null) {
        throw new NotFoundException();
      }

      return updatedPerson;
    }

    throw new ForbiddenException("Not authorised");
  }
}
