package io.featurehub.mr.rest;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.mr.api.PersonSecuredService;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.CreatePersonDetails;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RegistrationUrl;
import io.featurehub.mr.model.SearchPersonResult;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;

@Singleton
public class PersonResource implements PersonSecuredService {
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
  public RegistrationUrl createPerson(CreatePersonDetails createPersonDetails, Boolean includeGroups, SecurityContext context) {

    Person currentUser = authManager.from(context);

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

  @Override
  public Boolean deletePerson(String id, Boolean includeGroups, Boolean includeAcls, SecurityContext context) {
    if (authManager.isOrgAdmin(authManager.from(context))) {
      if (id.contains("@")) {
        return personApi.delete(id);
      } else {
        Person p = getPerson(id, includeAcls, includeGroups, context);
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
  public SearchPersonResult findPeople(Boolean includeGroups, SortOrder order, String filter, Integer startAt, Integer pageSize, SecurityContext context) {
    Person currentUser = authManager.from(context);

    if (authManager.isAnyAdmin(currentUser.getId().getId())) {

      int start = startAt == null ? 0 : startAt;
      int page = pageSize == null ? 20 : pageSize;

      PersonApi.PersonPagination pp = personApi.search(filter, order, start, page, new Opts().add(FillOpts.Groups, includeGroups));

      return new SearchPersonResult().people(pp.people).max(pp.max);
    }

    throw new ForbiddenException("Not admin");
  }

  private Opts peopleOpts(Boolean includeAcls, Boolean includeGroups) {
    return new Opts()
      .add(FillOpts.Groups, includeGroups)
      .add(FillOpts.Acls, includeAcls);
  }

  @Override
  public Person getPerson(String id, Boolean includeAcls, Boolean includeGroups, SecurityContext securityContext) {
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
  public Person updatePerson(String id, @NotNull @Valid Person person, Boolean includeGroups, Boolean includeAcls, SecurityContext securityContext) {
    Person from = authManager.from(securityContext);

    if (authManager.isAnyAdmin(from)) {
      Person updatedPerson = null;
      try {
        updatedPerson = personApi.update(id, person,
          peopleOpts(includeAcls, includeGroups),
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
