package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonType;
import io.featurehub.mr.model.SortOrder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PersonApi {

  @Nullable
  Person update(@NotNull UUID id, @NotNull Person person, @NotNull Opts opts, @NotNull UUID updatedBy) throws OptimisticLockingException;

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

  @NotNull PersonPagination search(@Nullable String filter, @Nullable SortOrder sortOrder, int offset, int max,
                                   @NotNull Set<PersonType> personTypes,
                                   Opts opts);

  @Nullable Person get(@NotNull UUID id, Opts opts);
  @Nullable Person get(@NotNull String email, Opts opts);

  @Nullable Person getByToken(@NotNull String id, Opts opts);

  @Nullable PersonToken create(@NotNull String email, @Nullable String name, @Nullable UUID createdBy) throws DuplicatePersonException;
  @Nullable CreatedServicePerson createServicePerson(@NotNull String name, @Nullable UUID createdBy);
  @Nullable CreatedServicePerson resetServicePersonToken(@NotNull UUID serviceAccountId);

  boolean delete(@NotNull String email);
}
