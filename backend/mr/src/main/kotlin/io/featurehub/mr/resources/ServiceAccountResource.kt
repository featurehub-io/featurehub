package io.featurehub.mr.resources

import io.featurehub.db.FilterOptType
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.db.services.Conversions
import io.featurehub.mr.api.ServiceAccountServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreateServiceAccount
import io.featurehub.mr.model.ResetApiKeyType
import io.featurehub.mr.model.ServiceAccount
import io.featurehub.mr.model.ServiceAccountPermission
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class ServiceAccountResource @Inject constructor(
  private val authManager: AuthManagerService,
  private val serviceAccountApi: ServiceAccountApi
) : ServiceAccountServiceDelegate {
  override fun createServiceAccountInPortfolio(
    id: UUID, serviceAccount: CreateServiceAccount,
    holder: ServiceAccountServiceDelegate.CreateServiceAccountInPortfolioHolder, securityContext: SecurityContext
  ): ServiceAccount {
    val person = authManager.from(securityContext)
    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      return try {
        serviceAccountApi.create(
          id,
          person,
          serviceAccount,
          Opts().add(FillOpts.Permissions, holder.includePermissions)
        )!!
      } catch (e: ServiceAccountApi.DuplicateServiceAccountException) {
        log.warn("Attempt to create duplicate service account {}", serviceAccount.name)
        throw WebApplicationException(Response.Status.CONFLICT)
      }
    }
    throw ForbiddenException()
  }

  override fun deleteServiceAccount(id: UUID, securityContext: SecurityContext?): Boolean {
    val person = authManager.from(securityContext)
    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      if (serviceAccountApi.delete(person, id)) {
        return true
      }
      throw NotFoundException()
    }
    throw ForbiddenException()
  }

  override fun getServiceAccount(
    id: String,
    holder: ServiceAccountServiceDelegate.GetServiceAccountHolder,
    securityContext: SecurityContext?
  ): ServiceAccount {
    val serviceAccountId = if ("self" == id) {
      val account =
        authManager.serviceAccount(securityContext) ?: throw NotFoundException("This is not a service account")
      account.id
    } else {
      Conversions.checkUuid(id) ?: throw NotFoundException("Not a valid UUID")
    }

    val info = serviceAccountApi[serviceAccountId, Opts().add(FillOpts.Permissions, holder.includePermissions)
      .add(FilterOptType.Application, holder.byApplicationId)]
      ?: throw NotFoundException()
    val person = authManager.from(securityContext)
    if (!authManager.isPortfolioAdmin(info.portfolioId, person) && !authManager.isOrgAdmin(person)) {
      throw ForbiddenException()
    }
    return info
  }

  override fun resetApiKey(
    id: UUID,
    holder: ServiceAccountServiceDelegate.ResetApiKeyHolder,
    securityContext: SecurityContext
  ): ServiceAccount {
    val person = authManager.from(securityContext)
    val info = serviceAccountApi[id, Opts.empty()] ?: throw NotFoundException()
    if (!authManager.isPortfolioAdmin(info.portfolioId, person) && !authManager.isOrgAdmin(person)) {
      throw ForbiddenException()
    }
    return serviceAccountApi.resetApiKey(
      id,
      holder.apiKeyType != ResetApiKeyType.SERVER_EVAL_ONLY,
      holder.apiKeyType != ResetApiKeyType.CLIENT_EVAL_ONLY
    ) ?: throw NotFoundException()
  }

  override fun searchServiceAccountsInPortfolio(
    id: UUID, holder: ServiceAccountServiceDelegate.SearchServiceAccountsInPortfolioHolder,
    securityContext: SecurityContext
  ): List<ServiceAccount> {
    val person = authManager.from(securityContext)
    val serviceAccounts = serviceAccountApi.search(
      id, holder.filter, holder.applicationId,
      person,
      Opts().add(FillOpts.ServiceAccountPermissionFilter)
        .add(FillOpts.Permissions, holder.includePermissions)
        .add(FillOpts.SdkURL, holder.includeSdkUrls)
    ).toMutableList()
    serviceAccounts.sortWith(Comparator.comparing { obj: ServiceAccount -> obj.name })
    return serviceAccounts
  }

  override fun updateServiceAccountOnPortfolio(
    id: UUID,
    serviceAccount: ServiceAccount,
    holder: ServiceAccountServiceDelegate.UpdateServiceAccountOnPortfolioHolder,
    securityContext: SecurityContext?
  ): ServiceAccount {
    val person = authManager.who(securityContext)

    val envIds = serviceAccount.permissions.map { obj: ServiceAccountPermission -> obj.environmentId }.toSet()

    if (envIds.size < serviceAccount.permissions.size) {
      throw BadRequestException("Duplicate environment ids were passed.")
    }

    if (authManager.isPortfolioAdmin(id, person) || authManager.isOrgAdmin(person)) {
      return try {
        serviceAccountApi.update(
          id,
          person,
          serviceAccount,
          Opts().add(FillOpts.Permissions, holder.includePermissions)
        ) ?: throw NotFoundException()
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      }
    }

    throw ForbiddenException()
  }

  @Deprecated("Deprecated in Java")
  override fun updateServiceAccount(
    serviceAccountId: UUID, serviceAccount: ServiceAccount,
    holder: ServiceAccountServiceDelegate.UpdateServiceAccountHolder, securityContext: SecurityContext
  ): ServiceAccount {
    val person = authManager.from(securityContext)

    val envIds = serviceAccount.permissions.map { obj: ServiceAccountPermission -> obj.environmentId }.toSet()

    if (envIds.size < serviceAccount.permissions.size) {
      throw BadRequestException("Duplicate environment ids were passed.")
    }

    if (authManager.isPortfolioAdmin(serviceAccount.portfolioId, person) || authManager.isOrgAdmin(person)) {
      return try {
        serviceAccountApi.update(
          serviceAccountId,
          person,
          serviceAccount,
          Opts().add(FillOpts.Permissions, holder.includePermissions)
        ) ?: throw NotFoundException()
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      }
    }

    throw ForbiddenException()
  }

  companion object {
    private val log = LoggerFactory.getLogger(ServiceAccountResource::class.java)
  }
}
