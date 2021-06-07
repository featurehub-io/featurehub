package io.featurehub.db.api;

import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.RoleType;
import io.featurehub.mr.model.SortOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EnvironmentApi {
  EnvironmentRoles personRoles(Person current, UUID eid);

  List<Environment> setOrdering(Application app, List<Environment> environments);

  class DuplicateEnvironmentException extends Exception {}
  class InvalidEnvironmentChangeException extends Exception {}
  boolean delete(UUID id);
  Environment get(UUID id, Opts opts, Person current);
  Environment update(UUID envId, Environment env, Opts opts) throws OptimisticLockingException, DuplicateEnvironmentException, InvalidEnvironmentChangeException;
  Environment create(Environment env, Application app, Person whoCreated) throws DuplicateEnvironmentException, InvalidEnvironmentChangeException;
  List<Environment> search(UUID appId, String filter, SortOrder order, Opts opts, Person current);
  Portfolio findPortfolio(UUID envId);
}
