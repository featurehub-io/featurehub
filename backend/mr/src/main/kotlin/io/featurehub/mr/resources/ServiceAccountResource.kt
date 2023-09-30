package io.featurehub.mr.resources

import io.featurehub.db.FilterOptType
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.api.ServiceAccountApi
import io.featurehub.mr.api.ServiceAccountServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
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
import java.util.stream.Collectors

class ServiceAccountResource @Inject constructor(
    private val authManager: AuthManagerService,
    private val serviceAccountApi: ServiceAccountApi
) : ServiceAccountServiceDelegate {
    override fun createServiceAccountInPortfolio(
        id: UUID, serviceAccount: ServiceAccount,
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

    override fun deleteServiceAccount(
        id: UUID,
        holder: ServiceAccountServiceDelegate.DeleteServiceAccountHolder,
        securityContext: SecurityContext
    ): Boolean {
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
        id: UUID,
        holder: ServiceAccountServiceDelegate.GetServiceAccountHolder,
        securityContext: SecurityContext
    ): ServiceAccount {
        var id = id
        if ("self" == id) {
            val account = authManager.serviceAccount(securityContext)
            id = account.id!!
        }
        val info = serviceAccountApi[id, Opts().add(FillOpts.Permissions, holder.includePermissions)
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
        ) ?: return ArrayList()
        serviceAccounts.sort(Comparator.comparing { obj: ServiceAccount -> obj.name })
        return serviceAccounts
    }

    override fun updateServiceAccount(
        serviceAccountId: UUID, serviceAccount: ServiceAccount,
        holder: ServiceAccountServiceDelegate.UpdateServiceAccountHolder, securityContext: SecurityContext
    ): ServiceAccount {
        val person = authManager.from(securityContext)
        val envIds = serviceAccount.permissions!!.stream().map { obj: ServiceAccountPermission -> obj.environmentId }
            .collect(Collectors.toSet())
        if (envIds.size < serviceAccount.permissions!!.size) {
            throw BadRequestException("Duplicate environment ids were passed.")
        }
        if (serviceAccount.portfolioId == null) {
            throw BadRequestException("No portfolio passed")
        }
        if (authManager.isPortfolioAdmin(serviceAccount.portfolioId, person) || authManager.isOrgAdmin(person)) {
            var result: ServiceAccount? = null
            result = try {
                serviceAccountApi.update(
                    serviceAccountId,
                    person,
                    serviceAccount,
                    Opts().add(FillOpts.Permissions, holder.includePermissions)
                )
            } catch (e: OptimisticLockingException) {
                throw WebApplicationException(422)
            }
            if (result == null) {
                throw NotFoundException()
            }
            return result
        }
        throw ForbiddenException()
    }

    companion object {
        private val log = LoggerFactory.getLogger(ServiceAccountResource::class.java)
    }
}
