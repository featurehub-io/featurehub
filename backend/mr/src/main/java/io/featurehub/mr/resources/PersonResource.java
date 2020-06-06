package io.featurehub.mr.resources;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.mr.api.PersonServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.CreatePersonDetails;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RegistrationUrl;
import io.featurehub.mr.model.SearchPersonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;

public class PersonResource implements PersonServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(PersonResource.class);
  private final PersonApi personApi;
  private final GroupApi groupApi;
  private final AuthManagerService authManager;

  @ConfigKey("register.url")
  private String registrationUrl;

  @Inject
  public PersonResource(PersonApi personApi, GroupApi groupApi, AuthManagerService authManager) {
    this.personApi = personApi;
    this.groupApi = groupApi;
    this.authManager = authManager;

    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public RegistrationUrl createPerson(CreatePersonDetails createPersonDetails, CreatePersonHolder holder, SecurityContext securityContext) {
    Person currentUser = authManager.from(securityContext);

    if (authManager.isAnyAdmin(currentUser.getId().getId())) {
      //create new user in the db
      try {
        PersonApi.PersonToken person = personApi.create(createPersonDetails.getEmail(), currentUser.getId().getId());

        //add user to the groups
        Optional.of(createPersonDetails.getGroupIds()).ifPresent(list -> list.forEach(id -> {
          groupApi.addPersonToGroup(id, person.id, Opts.empty());
        }));

        if(person == null) {
          throw new BadRequestException();
        }

        //return registration url
        RegistrationUrl regUrl = new RegistrationUrl();
        regUrl.setRegistrationUrl(String.format(registrationUrl, person.token));
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

    if ("self".equals(id)) {
      id = currentUser.getId().getId();
      log.info("User requested their own details: {}", id);
    }

    if (currentUser.getId().getId().equals(id) || authManager.isAnyAdmin(currentUser.getId().getId())) {
      Person person = personApi.get(id, peopleOpts(includeAcls, includeGroups));

      if (person == null) {
        throw new NotFoundException();
      }

      return person;
    }

    throw new ForbiddenException("nyet");
  }

  @Override
  public Boolean deletePerson(String id, DeletePersonHolder holder, SecurityContext securityContext) {
    if (authManager.isOrgAdmin(authManager.from(securityContext))) {
      if (id.contains("@")) {
        return personApi.delete(id);
      } else {
        Person p = getPerson(id, holder.includeAcls, holder.includeGroups, securityContext);
        if (p != null) {
          return personApi.delete(p.getEmail());
        } else {
          return false;
        }
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

      PersonApi.PersonPagination pp = personApi.search(holder.filter, holder.order, start, page, new Opts().add(FillOpts.Groups, holder.includeGroups));

      return new SearchPersonResult().people(pp.people).max(pp.max);
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
        updatedPerson = personApi.update(id, person,
          peopleOpts(holder.includeAcls, holder.includeGroups),
          from.getId().getId());
      } catch (OptimisticLockingException e) {
        throw new WebApplicationException(422);
      }

      if (updatedPerson == null) {
        throw new NotFoundException();
      }

      return updatedPerson;
    }

    throw new ForbiddenException("Not authorised");
  }
}
