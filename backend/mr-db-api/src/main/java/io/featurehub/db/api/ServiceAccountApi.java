package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.ServiceAccount;

import java.util.List;

public interface ServiceAccountApi {
  ServiceAccount get(String id, Opts opts);

  ServiceAccount update(String serviceAccountId, Person updater, ServiceAccount serviceAccount, Opts opts) throws OptimisticLockingException;

  /**
   * This has to determine if this user has access based on what they are asking for. If they have any access to the
   * portfolio, they should be able to see the service accounts. If they have access to specific environments they
   * should see permissions for each of those service accounts for those environments and sdk-urls.
   */
  List<ServiceAccount> search(String portfolioId, String filter, String applicationId, Person currentPerson, Opts opts);

  ServiceAccount resetApiKey(String id);

  class DuplicateServiceAccountException extends Exception {}

  ServiceAccount create(String portfolioId, Person creator, ServiceAccount serviceAccount, Opts opts) throws DuplicateServiceAccountException;

  Boolean delete(Person deleter, String serviceAccountId);

  void cleanupServiceAccountApiKeys();
}
