package io.featurehub.mr.resources

import io.featurehub.db.api.*
import io.featurehub.mr.api.EnvironmentServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Application
import io.featurehub.mr.model.CreateEnvironment
import io.featurehub.mr.model.Environment
import io.featurehub.mr.model.UpdateEnvironmentV2
import io.featurehub.mr.utils.ApplicationUtils
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class EnvironmentResource @Inject constructor(
    private val authManager: AuthManagerService,
    private val environmentApi: EnvironmentApi,
    private val applicationApi: ApplicationApi,
    private val applicationUtils: ApplicationUtils
) : EnvironmentServiceDelegate {

  override fun createEnvironment(
    id: UUID,
    environment: CreateEnvironment,
    securityContext: SecurityContext?
  ): Environment {
    val current = authManager.from(securityContext)
    var hasPermission = authManager.isOrgAdmin(current)
    if (!hasPermission) {
      val application = applicationApi.getApplication(id, Opts.empty()) ?: throw NotFoundException()
      hasPermission = authManager.isPortfolioAdmin(application.portfolioId, current, null)
    }
    if (hasPermission) {
      return try {
        environmentApi.create(environment, id, current) ?: throw NotFoundException()
      } catch (e: EnvironmentApi.DuplicateEnvironmentException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (e: EnvironmentApi.InvalidEnvironmentChangeException) {
        throw BadRequestException()
      }
    }
    throw ForbiddenException()
  }

  override fun createEnvironmentOld(
    id: UUID,
    createEnvironment: CreateEnvironment,
    securityContext: SecurityContext?
  ): CreateEnvironment {
    TODO("Not yet implemented")
  }

  override fun deleteEnvironment(
        eid: UUID,
        holder: EnvironmentServiceDelegate.DeleteEnvironmentHolder,
        securityContext: SecurityContext
    ): Boolean {
        val current = authManager.from(securityContext)
        return if (authManager.isOrgAdmin(current) ||
            authManager.isPortfolioAdmin(environmentApi.findPortfolio(eid), current)
        ) {
            environmentApi.delete(eid)
        } else false
    }

    override fun environmentOrdering(
        id: UUID, environments: List<Environment>,
        securityContext: SecurityContext
    ): List<Environment> {
        val perm = applicationUtils.check(securityContext, id)
        return environmentApi.setOrdering(perm.app, environments) ?: throw BadRequestException()
    }

    override fun findEnvironments(
        id: UUID,
        holder: EnvironmentServiceDelegate.FindEnvironmentsHolder,
        securityContext: SecurityContext
    ): List<Environment> {
        val current = authManager.from(securityContext)
        return environmentApi.search(
            id, holder.filter, holder.order,
            Opts().add(FillOpts.Acls, holder.includeAcls)
                .add(FillOpts.Features, holder.includeFeatures)
                .add(FillOpts.Details, holder.includeDetails), current
        )
    }

    override fun getEnvironment(
        eid: UUID,
        holder: EnvironmentServiceDelegate.GetEnvironmentHolder,
        securityContext: SecurityContext
    ): Environment {
        val current = authManager.from(securityContext)
        val found = environmentApi[eid, Opts()
            .add(FillOpts.Acls, holder.includeAcls)
            .add(FillOpts.Features, holder.includeFeatures)
            .add(FillOpts.ServiceAccounts, holder.includeServiceAccounts)
            .add(FillOpts.SdkURL, holder.includeSdkUrl)
            .add(FillOpts.Details, holder.includeDetails), current]
        if (found == null) {
            log.warn("User had no access to environment `{}` or it didn't exist.", eid)
            throw ForbiddenException()
        }
        return found
    }

    @Deprecated("Deprecated in Java")
    override fun updateEnvironment(
        eid: UUID, environment: Environment, holder: EnvironmentServiceDelegate.UpdateEnvironmentHolder,
        securityContext: SecurityContext
    ): Environment {
        val current = authManager.from(securityContext)
        if (authManager.isOrgAdmin(current) ||
            authManager.isPortfolioAdmin(environmentApi.findPortfolio(environment.id), current)
        ) {
            val update: Environment?
            update = try {
                environmentApi.update(
                    eid, environment, Opts().add(
                        FillOpts.Acls,
                        holder.includeAcls
                    ).add(FillOpts.Features, holder.includeFeatures).add(FillOpts.Details, holder.includeDetails)
                )
            } catch (e: OptimisticLockingException) {
                throw WebApplicationException(422)
            } catch (e: EnvironmentApi.DuplicateEnvironmentException) {
                throw WebApplicationException(Response.Status.CONFLICT)
            } catch (e: EnvironmentApi.InvalidEnvironmentChangeException) {
                throw BadRequestException()
            }
            if (update == null) {
                throw NotFoundException()
            }
            return update
        }
        throw ForbiddenException()
    }

  override fun updateEnvironmentOnApplication(
    id: UUID,
    updateEnvironmentV2: UpdateEnvironmentV2,
    holder: EnvironmentServiceDelegate.UpdateEnvironmentOnApplicationHolder,
    securityContext: SecurityContext?
  ): Environment {
    TODO("Not yet implemented")
  }

  companion object {
        private val log = LoggerFactory.getLogger(EnvironmentResource::class.java)
    }
}
