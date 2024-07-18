package io.featurehub.db.api

import io.featurehub.mr.model.CreateServiceAccount
import io.featurehub.mr.model.Person
import io.featurehub.mr.model.ServiceAccount
import java.util.*

interface ServiceAccountApi {
  operator fun get(id: UUID, opts: Opts): ServiceAccount?

  @Throws(OptimisticLockingException::class)
  fun update(serviceAccountId: UUID, updater: Person, serviceAccount: ServiceAccount, appId: UUID?, opts: Opts): ServiceAccount?
  @Throws(OptimisticLockingException::class)
  fun update(portfolioId: UUID, personId: UUID, serviceAccount: ServiceAccount, appId: UUID?, opts: Opts): ServiceAccount?

  /**
   * This has to determine if this user has access based on what they are asking for. If they have any access to the
   * portfolio, they should be able to see the service accounts. If they have access to specific environments they
   * should see permissions for each of those service accounts for those environments and sdk-urls.
   */
  fun search(
    portfolioId: UUID,
    filter: String?,
    applicationId: UUID?,
    currentPerson: Person,
    opts: Opts
  ): List<ServiceAccount>

  fun resetApiKey(id: UUID, resetClientEvalApiKey: Boolean, resetServerEvalApiKey: Boolean): ServiceAccount?
  class DuplicateServiceAccountException : Exception()

  @Throws(DuplicateServiceAccountException::class)
  fun create(portfolioId: UUID, creator: Person, serviceAccount: CreateServiceAccount, opts: Opts): ServiceAccount?
  fun delete(deleter: Person, serviceAccountId: UUID): Boolean
  fun cleanupServiceAccountApiKeys()

  /**
   * This will unpublish the listed service accounts or all of them if none are specified
   */
  fun unpublishServiceAccounts(portfolioId: UUID, serviceAccounts: List<UUID>?): Int

  fun findServiceAccountByUserId(personId: UUID): UUID?
}
