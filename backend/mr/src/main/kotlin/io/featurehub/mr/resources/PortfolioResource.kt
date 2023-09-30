package io.featurehub.mr.resources

import io.featurehub.db.api.*
import io.featurehub.mr.api.PortfolioServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.Group
import io.featurehub.mr.model.Portfolio
import io.featurehub.mr.utils.PortfolioUtils
import jakarta.inject.Inject
import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.slf4j.LoggerFactory
import java.util.*

class PortfolioResource @Inject constructor(
    private val groupApi: GroupApi,
    private val authManager: AuthManagerService,
    private val portfolioApi: PortfolioApi,
    private val organizationApi: OrganizationApi,
    private val portfolioUtils: PortfolioUtils
) : PortfolioServiceDelegate {
    fun createPortfolio(
        portfolio: Portfolio?,
        holder: PortfolioServiceDelegate.CreatePortfolioHolder,
        securityContext: SecurityContext?
    ): Portfolio {
        if (authManager.isOrgAdmin(authManager.from(securityContext))) {
            val opts =
                Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.Applications, holder.includeApplications)
            val created: Portfolio?
            created = try {
                portfolioApi.createPortfolio(portfolio, opts, authManager.from(securityContext))
            } catch (e: PortfolioApi.DuplicatePortfolioException) {
                log.error("Duplicate portfolio name", e)
                throw WebApplicationException(Response.Status.CONFLICT)
            }
            if (created == null) {
                throw NotFoundException("No conditions to allow you to create.")
            }
            val group: Group?
            group = try {
                groupApi.createGroup(
                    created.id!!,
                    Group()
                        .name(portfolioUtils.formatPortfolioAdminGroupName(portfolio))
                        .admin(true)
                        .portfolioId(created.id),
                    authManager.from(securityContext)
                )
            } catch (e: GroupApi.DuplicateGroupException) {
                throw WebApplicationException(Response.Status.CONFLICT)
            }
            created.addGroupsItem(group)
            return created
        }
        throw ForbiddenException("No permission to create portfolio")
    }

    override fun deletePortfolio(id: UUID, holder: PortfolioServiceDelegate.DeletePortfolioHolder, securityContext: SecurityContext): Boolean {
        val from = authManager.from(securityContext)
        if (authManager.isOrgAdmin(from)) {
            return if (portfolioApi.getPortfolio(id, Opts.empty(), from) != null) {
                portfolioApi.deletePortfolio(id)
                true
            } else {
                false // no portfolio
            }
        }
        throw ForbiddenException("Not allowed to delete portfolio")
    }

    override fun findPortfolios(holder: PortfolioServiceDelegate.FindPortfoliosHolder, securityContext: SecurityContext): List<Portfolio> {
        return portfolioApi.findPortfolios(
            holder.filter,
            holder.order,
            Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.Applications, holder.includeApplications),
            authManager.from(securityContext)
        )
    }

    override fun getPortfolio(
        id: UUID,
        holder: PortfolioServiceDelegate.GetPortfolioHolder,
        securityContext: SecurityContext
    ): Portfolio {
        return portfolioApi.getPortfolio(
            id,
            Opts().add(FillOpts.Groups, holder.includeGroups)
                .add(FillOpts.Applications, holder.includeApplications)
                .add(FillOpts.Environments, holder.includeEnvironments), authManager.from(securityContext)
        ) ?: throw NotFoundException("No such portfolio")
    }

    override fun updatePortfolio(
        id: UUID, portfolio: Portfolio, holder: PortfolioServiceDelegate.UpdatePortfolioHolder,
        securityContext: SecurityContext
    ): Portfolio {
        val current = authManager.from(securityContext)
        if (authManager.isPortfolioAdmin(id, current, null)) {
            portfolio.id = id
            val pf: Portfolio?
            pf = try {
                portfolioApi.updatePortfolio(portfolio, Opts.empty())
            } catch (e: PortfolioApi.DuplicatePortfolioException) {
                log.error("Duplicate portfolio name", e)
                throw WebApplicationException(Response.Status.CONFLICT)
            } catch (e: OptimisticLockingException) {
                throw WebApplicationException(422)
            }
            if (pf == null) {
                throw NotFoundException("No such portfolio")
            }
            groupApi.updateAdminGroupForPortfolio(pf.id!!, portfolioUtils.formatPortfolioAdminGroupName(pf))
            return portfolioApi.getPortfolio(
                pf.id, Opts().add(FillOpts.Groups, holder.includeGroups)
                    .add(FillOpts.Applications, holder.includeApplications)
                    .add(FillOpts.Environments, holder.includeEnvironments), current
            )
        }
        throw ForbiddenException("Not an admin, cannot rename portfolio")
    }

    companion object {
        private val log = LoggerFactory.getLogger(PortfolioResource::class.java)
    }
}
