package io.featurehub.db.api;

import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.ServiceAccount;

import java.util.List;
import java.util.UUID;

public interface ServiceAccountApi {
  ServiceAccount get(UUID id, Opts opts);

  ServiceAccount update(UUID serviceAccountId, Person updater, ServiceAccount serviceAccount, Opts opts) throws OptimisticLockingException;

  /**
   * This has to determine if this user has access based on what they are asking for. If they have any access to the
   * portfolio, they should be able to see the service accounts. If they have access to specific environments they
   * should see permissions for each of those service accounts for those environments and sdk-urls.
   */
  List<ServiceAccount> search(UUID portfolioId, String filter, UUID applicationId, Person currentPerson, Opts opts);

  ServiceAccount resetApiKey(UUID id, boolean resetClientEvalApiKey, boolean resetServerEvalApiKey);

  class DuplicateServiceAccountException extends Exception {}

  ServiceAccount create(UUID portfolioId, Person creator, ServiceAccount serviceAccount, Opts opts) throws DuplicateServiceAccountException;

  Boolean delete(Person deleter, UUID serviceAccountId);

  void cleanupServiceAccountApiKeys();
}
