package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;

import java.util.List;

public interface PersonApi {

  Person update(String id, Person person, Opts opts, String updatedBy) throws OptimisticLockingException;

  class PersonPagination {
    public int max;
    public List<Person> people;
  }

  class PersonToken {
    public String token;
    public String id;

    public PersonToken(String token, String id) {
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

  Person get(@NotNull String id, Opts opts);

  Person getByToken(@NotNull String id, Opts opts);

  PersonToken create(@NotNull String email, @NotNull String name, String createdBy) throws DuplicatePersonException;

  boolean delete(@NotNull String email);
}
