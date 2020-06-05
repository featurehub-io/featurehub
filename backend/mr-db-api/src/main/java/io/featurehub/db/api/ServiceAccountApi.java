package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.ServiceAccount;

import java.util.List;

public interface ServiceAccountApi {
  ServiceAccount get(String id, Opts opts);

  ServiceAccount update(String id, Person updater, ServiceAccount serviceAccount, Opts opts) throws OptimisticLockingException;

  List<ServiceAccount> search(String portfolioId, String filter, String applicationId, Opts opts);

  ServiceAccount resetApiKey(String id);

  class DuplicateServiceAccountException extends Exception {}

  ServiceAccount create(String portfolioId, Person creator, ServiceAccount serviceAccount, Opts opts) throws DuplicateServiceAccountException;

  Boolean delete(Person deleter, String serviceAccountId);
}
