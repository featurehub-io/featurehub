package io.featurehub.mr.resources

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.featurehub.db.api.*
import io.featurehub.mr.api.ApplicationServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.*
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

class ApplicationResource @Inject constructor(
    private val authManager: AuthManagerService,
    private val applicationApi: ApplicationApi,
    private val environmentApi: EnvironmentApi,
    private val applicationUtils: ApplicationUtils
) : ApplicationServiceDelegate {
    @ConfigKey("environment.production.name")
    var productionEnvironmentName = "production"

    @ConfigKey("environment.production.desc")
    var productionEnvironmentDescription = "production"

    init {
        DeclaredConfigResolver.resolve(this)
    }

    override fun applicationPermissions(id: UUID, securityContext: SecurityContext): ApplicationPermissions {
        val current = authManager.from(securityContext)
        return applicationApi.findApplicationPermissions(id, current.id!!.id)
    }

  override fun createApplication(
    id: UUID,
    application: CreateApplication,
    holder: ApplicationServiceDelegate.CreateApplicationHolder,
    securityContext: SecurityContext?
  ): Application {
    val current = authManager.from(securityContext)
    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(id, current, null)) {
      return try {
        var app = applicationApi.createApplication(id, application, current) ?: throw NotFoundException()

        environmentApi.create(
          CreateEnvironment().name(productionEnvironmentName).production(true)
            .description(productionEnvironmentDescription), app.id, current
        )

        if (java.lang.Boolean.TRUE == holder.includeEnvironments) {
          app = applicationApi.getApplication(app.id, Opts.opts(FillOpts.Environments)) ?: throw NotFoundException()
        }

        app
      } catch (e: ApplicationApi.DuplicateApplicationException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (e: EnvironmentApi.DuplicateEnvironmentException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (e: EnvironmentApi.InvalidEnvironmentChangeException) {
        log.error("Failed to change environment", e)
        throw BadRequestException()
      }
    }
    throw ForbiddenException("No permission to add application")
  }

    override fun deleteApplication(
        eid: UUID,
        holder: ApplicationServiceDelegate.DeleteApplicationHolder,
        securityContext: SecurityContext
    ): Boolean {
        val apc = applicationUtils.check(securityContext, eid)
        return applicationApi.deleteApplication(apc.app.portfolioId, apc.app.id)
    }

    override fun findApplications(
        id: UUID,
        holder: ApplicationServiceDelegate.FindApplicationsHolder,
        securityContext: SecurityContext
    ): List<Application> {
        val from = authManager.from(securityContext)
        return applicationApi.findApplications(
            id,
            holder.filter,
            holder.order,
            Opts().add(FillOpts.Environments, holder.includeEnvironments)
                .add(FillOpts.Features, holder.includeFeatures),
            from,
            authManager.isOrgAdmin(from) || authManager.isPortfolioAdmin(id, from, null)
        )
    }

    override fun getApplication(
        appId: UUID,
        holder: ApplicationServiceDelegate.GetApplicationHolder,
        securityContext: SecurityContext
    ): Application {
        // they must be at least able to read features
        applicationUtils.featureReadCheck(securityContext, appId)
        return applicationApi.getApplication(
            appId, Opts().add(
                FillOpts.Environments,
                holder.includeEnvironments
            )
        ) ?: throw NotFoundException()
    }

    override fun summaryApplication(appId: UUID, securityContext: SecurityContext): ApplicationSummary {
        // they must be at least able to read features
        applicationUtils.featureReadCheck(securityContext, appId)
        return applicationApi.getApplicationSummary(appId)!!
    }

    override fun updateApplication(
        appId: UUID, application: Application, holder: ApplicationServiceDelegate.UpdateApplicationHolder,
        securityContext: SecurityContext
    ): Application {
        applicationUtils.check(securityContext, appId)
        return try {
            applicationApi.updateApplication(
                appId,
                application,
                Opts().add(FillOpts.Environments, holder.includeEnvironments)
            )!!
        } catch (e: ApplicationApi.DuplicateApplicationException) {
            throw WebApplicationException(Response.Status.CONFLICT)
        } catch (e: OptimisticLockingException) {
            throw WebApplicationException(422)
        }
    }

  override fun updateApplicationOnPortfolio(
    id: UUID,
    application: Application,
    holder: ApplicationServiceDelegate.UpdateApplicationOnPortfolioHolder,
    securityContext: SecurityContext?
  ): Application {
    TODO("Not yet implemented")
  }

  companion object {
        private val log = LoggerFactory.getLogger(ApplicationResource::class.java)
    }
}
