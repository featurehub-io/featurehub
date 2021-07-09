package io.featurehub.db.services;

import io.ebean.Database;
import io.ebean.FutureList;
import io.ebean.FutureRowCount;
import io.ebean.annotation.Transactional;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.NotNull;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PersonApi;
import io.featurehub.db.model.DbGroup;
import io.featurehub.db.model.DbOrganization;
import io.featurehub.db.model.DbPerson;
import io.featurehub.db.model.query.QDbPerson;
import io.featurehub.db.password.PasswordSalter;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class PersonSqlApi implements PersonApi {
  private static final Logger log = LoggerFactory.getLogger(PersonSqlApi.class);
  private final Database database;
  private final Conversions convertUtils;
  public final static int MAX_SEARCH = 100;
  private final ArchiveStrategy archiveStrategy;
  private final PasswordSalter passwordSalter = new PasswordSalter();

  @Inject
  public PersonSqlApi(Database database, Conversions convertUtils, ArchiveStrategy archiveStrategy) {
    this.database = database;
    this.convertUtils = convertUtils;
    this.archiveStrategy = archiveStrategy;
  }

  @Override
  public Person update(UUID id, Person person, Opts opts, UUID updatedBy) throws OptimisticLockingException {
    Conversions.nonNullPersonId(id);
    Conversions.nonNullPerson(person);
    Conversions.nonNullPersonId(updatedBy);

    DbPerson adminPerson = convertUtils.byPerson(updatedBy, Opts.opts(FillOpts.Groups));

    DbPerson p = convertUtils.byPerson(id, opts);

    if (adminPerson != null && p != null && p.getWhenArchived() == null) {
      return updatePerson(person, opts, adminPerson, p);
    }

    return null;
  }

  @Override
  public boolean noUsersExist() {
    return !new QDbPerson().exists();
  }

  public Person updatePerson(Person person, Opts opts, DbPerson adminPerson, DbPerson p) throws OptimisticLockingException {
    if (person.getVersion() == null || p.getVersion() != person.getVersion()) {
      throw new OptimisticLockingException();
    }

    if (person.getName() != null) {
      p.setName(person.getName());
    }
    if (person.getEmail() != null) {
      p.setEmail(person.getEmail());
    }

    if (person.getGroups() != null) {
      // we are going to need their groups to determine what they can do
      Person admin = convertUtils.toPerson(adminPerson, Opts.opts(FillOpts.Groups));

      boolean adminSuperuser = admin.getGroups().stream().anyMatch(g -> g.getPortfolioId() == null && g.getAdmin());
      List<UUID> validPortfolios = admin.getGroups()
        .stream()
        .filter(g -> g.getPortfolioId() != null && g.getAdmin())
        .map(Group::getPortfolioId)
        .collect(Collectors.toList());

      if (!adminSuperuser && validPortfolios.isEmpty()) {
        return null; // why are they even here???
      }

      // TODO: if groups are passed, limit removal of groups to the organisation relevant

      List<DbGroup> removeGroups = new ArrayList<>();
      List<UUID> replacementGroupIds = person.getGroups().stream().map(Group::getId).collect(Collectors.toList());
      List<UUID> foundReplacementGroupIds = new ArrayList<>(); // these are the ones we found

      p.getGroupsPersonIn().forEach(g -> {
        if (replacementGroupIds.contains(g.getId())) { // this has been passed to us and it is staying
          foundReplacementGroupIds.add(g.getId());
        } else if (adminSuperuser || (g.getOwningPortfolio() != null && validPortfolios.contains(g.getOwningPortfolio().getId()))) {
          // only if the admin is a superuser or this is a group in one of their portfolios will we honour it
          removeGroups.add(g);
        } else {
          log.warn("No permission to remove group {} from user {}", g, p.getEmail());
        }
      });

      log.debug("Removing groups {} from user {}", removeGroups, p.getEmail());
      // now remove them from these groups, we know this is valid
      p.getGroupsPersonIn().removeAll(removeGroups);

      replacementGroupIds.removeAll(foundReplacementGroupIds); // now this should be id's that we should consider adding

      log.debug("Attempting to add groups {} to user {} ", replacementGroupIds, p.getEmail());
      // now we have to find the replacement groups and see if this user is allowed to add them
      replacementGroupIds.forEach(gid -> {
        DbGroup group = convertUtils.byGroup(gid, Opts.empty());
        if (group != null && (adminSuperuser ||
          (group.getOwningPortfolio() != null && validPortfolios.contains(group.getOwningPortfolio().getId())) )) {
          p.getGroupsPersonIn().add(group);
        } else {
          log.warn("No permission to add group {} to user {}", group, p.getEmail());
        }
      });
    }

    updatePerson(p);

    return convertUtils.toPerson(p, opts);
  }

  @Override
  public PersonPagination search(String filter, SortOrder sortOrder, int offset, int max, Opts opts) {
    offset = Math.max(offset, 0);

    max = Math.min(max, MAX_SEARCH);
    max = Math.max(max, 1);

    // set the limits
    QDbPerson search = new QDbPerson().setFirstRow(offset).setMaxRows(max);

    // set the filter if anything, make sure it is case insignificant
    if (filter != null) {
      // name is mixed case, email is always lower case
      search = search.or().name.icontains(filter).email.contains(filter.toLowerCase()).endOr();
    }


    if (sortOrder != null) {
      if (sortOrder == SortOrder.ASC) {
        search = search.order().name.asc();
      } else {
        search = search.order().name.desc();
      }
    }

    if (!opts.contains(FillOpts.Archived)) {
      search = search.whenArchived.isNull();
    }

    FutureRowCount<DbPerson> futureCount = search.findFutureCount();
    FutureList<DbPerson> futureList = search.findFutureList();

    PersonPagination pagination = new PersonPagination();

    try {
      pagination.max = futureCount.get();
      DbOrganization org = convertUtils.getDbOrganization();
      final List<DbPerson> dbPeople = futureList.get();
      pagination.people = dbPeople.stream().map(dbp ->
        convertUtils.toPerson(dbp, org, opts)
      ).collect(Collectors.toList());

      LocalDateTime now = LocalDateTime.now();

      pagination.personIdsWithExpiredTokens = dbPeople.stream()
        .filter(p -> p.getToken() != null && p.getTokenExpiry() != null && p.getTokenExpiry().isBefore(now))
        .map(DbPerson::getId)
        .collect(Collectors.toList());

      pagination.personsWithOutstandingTokens = dbPeople.stream()
        .filter(p -> p.getToken() != null)
        .map(p -> new PersonToken(p.getToken(), p.getId()))
        .collect(Collectors.toList());

      return pagination;
    } catch (InterruptedException | ExecutionException e) {
      log.error("Failed to execute search.", e);
      return null;
    }
  }

  @Override
  public Person get(@NotNull String email, Opts opts) {
    if (email == null) {
      throw new IllegalArgumentException("email required");
    }

    if (email.contains("@")) {
      QDbPerson search = new QDbPerson().email.eq(email.toLowerCase());
      if (!opts.contains(FillOpts.Archived)) {
        search = search.whenArchived.isNull();
      }
      return search.groupsPersonIn.fetch()
        .findOneOrEmpty()
        .map(p -> convertUtils.toPerson(p, opts))
        .orElse(null);
    }

    UUID id = Conversions.checkUuid(email);

    if (id != null) {
      return get(id, opts);
    }

    return null;
  }

  @Override
  public Person get(UUID id, Opts opts) {
    Conversions.nonNullPersonId(id);


    QDbPerson search = new QDbPerson().id.eq(id);
    if (!opts.contains(FillOpts.Archived)) {
      search = search.whenArchived.isNull();
    }
    return search.groupsPersonIn.fetch()
      .findOneOrEmpty()
      .map(p -> convertUtils.toPerson(p, opts))
      .orElse(null);

  }

  @Override
  public Person getByToken(String token, Opts opts) {
    if (token == null) return null;

    DbPerson person  = new QDbPerson().whenArchived.isNull().token.eq(token).findOne();

    if (person != null && person.getTokenExpiry().isAfter(getNow())) {
      return convertUtils.toPerson(person, opts);
    }
    return null;
  }

  protected LocalDateTime getNow() {
    return LocalDateTime.now();
  }

  @Override
  public PersonToken create(String email, String name, UUID createdBy) throws DuplicatePersonException {
    if (email == null || name == null) {
      return null;
    }

    DbPerson created = createdBy == null ? null : convertUtils.byPerson(createdBy);

    if (createdBy != null && created == null) {
      return null;
    }

    PersonToken personToken;
    final DbPerson onePerson = new QDbPerson().email.eq(email.toLowerCase()).findOne();
    if (onePerson == null) {

      String token = UUID.randomUUID().toString();
      DbPerson.Builder builder = new DbPerson.Builder()
        .email(email.toLowerCase())
        .name(name)
        .token(token)
        .tokenExpiry(getNow().plusDays(7));

      if (created != null) {
        builder.whoCreated(created);
      }

      DbPerson person = builder.build();
      updatePerson(person);

      personToken = new PersonToken(person.getToken(), person.getId());
    } else if (onePerson.getWhenArchived() != null) {
      onePerson.setWhenArchived(null);
      onePerson.setToken(UUID.randomUUID().toString()); // ensures it gets past registration again
      onePerson.setName(name);
      if (created != null) {
        onePerson.setWhoCreated(created);
      }
      updatePerson(onePerson);
      return null;
    } else {
      throw new DuplicatePersonException();
    }

    return personToken;
  }

  /**
   * This person will be fully formed, not token. Usually used only for testing.
   */
  Person createPerson(String email, String name, String password, UUID createdById, Opts opts) throws DuplicatePersonException {
    if (email == null) {
      return null;
    }

    DbPerson created = createdById == null ? null : convertUtils.byPerson(createdById);

    if (createdById != null && created == null) {
      return null;
    }

    if (new QDbPerson().email.eq(email.toLowerCase()).findOne() == null) {

      DbPerson.Builder builder = new DbPerson.Builder()
        .email(email.toLowerCase())
        .name(name);

      if (created != null) {
        builder.whoCreated(created);
      }

      DbPerson person = builder.build();
      passwordSalter.saltPassword(password).ifPresent(person::setPassword);

      updatePerson(person);

      return convertUtils.toPerson(person, opts);
    } else {
      throw new DuplicatePersonException();
    }
  }


  @Transactional
  private void updatePerson(DbPerson p) {
    database.save(p);
  }

  @Override
  public boolean delete(String email) {
    return new QDbPerson().email.eq(email.toLowerCase()).findOneOrEmpty().map(p -> {
      archiveStrategy.archivePerson(p);
      return true;
    }).orElse(false);
  }
}
