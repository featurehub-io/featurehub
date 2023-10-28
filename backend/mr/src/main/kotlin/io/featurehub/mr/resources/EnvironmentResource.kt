package io.featurehub.mr.resources

import io.featurehub.db.api.ApplicationApi
import io.featurehub.db.api.EnvironmentApi
import io.featurehub.db.api.FillOpts
import io.featurehub.db.api.OptimisticLockingException
import io.featurehub.db.api.Opts
import io.featurehub.db.exception.MissingEncryptionPasswordException
import io.featurehub.mr.api.EnvironmentServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
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

  @Deprecated("Deprecated in Java", ReplaceWith("createEnvironmentOnApplication(id, environment, securityContext)"))
  override fun createEnvironment(
    id: UUID,
    environment: CreateEnvironment,
    securityContext: SecurityContext?
  ): Environment {
    return createEnvironmentOnApplication(id, environment, securityContext)
  }

  override fun createEnvironmentOnApplication(
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

  override fun deleteEnvironment(
    eid: UUID,
    holder: EnvironmentServiceDelegate.DeleteEnvironmentHolder,
    securityContext: SecurityContext
  ): Boolean {
    val current = authManager.who(securityContext)
    return if (authManager.isOrgAdmin(current) ||
      authManager.isPortfolioAdminOfEnvironment(eid, current)
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
      .add(FillOpts.DecryptWebhookDetails, holder.decryptWebhookDetails)
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
      authManager.isPortfolioAdminOfEnvironment(environment.id, current)
    ) {
      val update = try {
        environmentApi.update(
          eid, environment, Opts().add(
            FillOpts.Acls,
            holder.includeAcls
          ).add(FillOpts.Features, holder.includeFeatures).add(FillOpts.Details, holder.includeDetails)
        ) ?: throw NotFoundException()
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (e: EnvironmentApi.DuplicateEnvironmentException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (e: EnvironmentApi.InvalidEnvironmentChangeException) {
        throw BadRequestException()
      }

      return update
    }
    throw ForbiddenException()
  }

  override fun updateEnvironmentOnApplication(
    id: UUID,
    environment: UpdateEnvironmentV2,
    holder: EnvironmentServiceDelegate.UpdateEnvironmentOnApplicationHolder,
    securityContext: SecurityContext?
  ): Environment {
    val person = authManager.who(securityContext)
    if (authManager.isOrgAdmin(person) || authManager.isPortfolioAdminOfEnvironment(environment.id, person)
    ) {
      return try {
        environmentApi.update(
          id, environment, Opts().add(FillOpts.Details, holder.includeDetails)
        ) ?: throw NotFoundException()
      } catch (e: MissingEncryptionPasswordException) {
        throw WebApplicationException(412)
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      } catch (e: EnvironmentApi.DuplicateEnvironmentException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (e: EnvironmentApi.InvalidEnvironmentChangeException) {
        throw BadRequestException()
      }
    }
    throw ForbiddenException()
  }

  companion object {
    private val log = LoggerFactory.getLogger(EnvironmentResource::class.java)
  }
}
