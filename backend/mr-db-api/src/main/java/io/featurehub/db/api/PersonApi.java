package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface PersonApi {

  Person update(UUID id, Person person, Opts opts, UUID updatedBy) throws OptimisticLockingException;

  // used to determine if the database has no user, which is possible if using external auth
  boolean noUsersExist();

  class PersonPagination {
    public int max;
    public List<Person> people;
    public List<PersonToken> personsWithOutstandingTokens;
    public List<UUID> personIdsWithExpiredTokens;
  }

  class PersonToken {
    public String token;
    public UUID id;

    public PersonToken(String token, UUID id) {
      this.token = token;
      this.id = id;
    }
  }

  class DuplicatePersonException extends Exception {
    public DuplicatePersonException() {
      super();
    }
  }

  PersonPagination search(String filter, @NotNull SortOrder sortOrder, int offset, int max, Opts opts);

  Person get(@NotNull UUID id, Opts opts);
  Person get(@NotNull String email, Opts opts);

  Person getByToken(@NotNull String id, Opts opts);

  PersonToken create(@NotNull String email, @NotNull String name, UUID createdBy) throws DuplicatePersonException;

  boolean delete(@NotNull String email);
}
