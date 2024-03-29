package io.featurehub.mr.resources

import io.featurehub.db.api.*
import io.featurehub.mr.api.PortfolioServiceDelegate
import io.featurehub.mr.auth.AuthManagerService
import io.featurehub.mr.model.CreateGroup
import io.featurehub.mr.model.CreatePortfolio
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

  override fun createPortfolio(
    createPortfolio: CreatePortfolio,
    holder: PortfolioServiceDelegate.CreatePortfolioHolder,
    securityContext: SecurityContext?
  ): Portfolio {
    if (authManager.isOrgAdmin(authManager.from(securityContext))) {
      val opts =
        Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.Applications, holder.includeApplications)
      val created = try {
        portfolioApi.createPortfolio(createPortfolio, opts, authManager.who(securityContext)) ?: throw NotFoundException()
      } catch (e: PortfolioApi.DuplicatePortfolioException) {
        log.error("Duplicate portfolio name", e)
        throw WebApplicationException(Response.Status.CONFLICT)
      }
      val group = try {
        groupApi.createGroup(
          created.id,
          CreateGroup()
            .name(portfolioUtils.formatPortfolioAdminGroupName(createPortfolio.name))
            .admin(true),
          authManager.from(securityContext)
        ) ?: throw NotFoundException()
      } catch (e: GroupApi.DuplicateGroupException) {
        throw WebApplicationException(Response.Status.CONFLICT)
      }
      created.addGroupsItem(group)
      return created
    }
    throw ForbiddenException("No permission to create portfolio")
  }

  override fun deletePortfolio(
    id: UUID,
    holder: PortfolioServiceDelegate.DeletePortfolioHolder,
    securityContext: SecurityContext
  ): Boolean {
    val from = authManager.who(securityContext)
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

  override fun findPortfolios(
    holder: PortfolioServiceDelegate.FindPortfoliosHolder,
    securityContext: SecurityContext
  ): List<Portfolio> {
    return portfolioApi.findPortfolios(
      holder.filter,
      holder.order,
      Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.Applications, holder.includeApplications),
      authManager.who(securityContext)
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
        .add(FillOpts.Environments, holder.includeEnvironments), authManager.who(securityContext)
    ) ?: throw NotFoundException("No such portfolio")
  }


  override fun updatePortfolioOnOrganisation(
    portfolio: Portfolio,
    holder: PortfolioServiceDelegate.UpdatePortfolioOnOrganisationHolder,
    securityContext: SecurityContext?
  ): Portfolio {
    val current = authManager.from(securityContext)

    if (authManager.isPortfolioAdmin(portfolio.id, current, null)) {
      val pf = try {
        portfolioApi.updatePortfolio(portfolio, Opts.empty()) ?: throw NotFoundException("No such portfolio")
      } catch (e: PortfolioApi.DuplicatePortfolioException) {
        log.error("Duplicate portfolio name", e)
        throw WebApplicationException(Response.Status.CONFLICT)
      } catch (e: OptimisticLockingException) {
        throw WebApplicationException(422)
      }

      groupApi.updateAdminGroupForPortfolio(pf.id, portfolioUtils.formatPortfolioAdminGroupName(pf.name))
      return portfolioApi.getPortfolio(
        pf.id, Opts().add(FillOpts.Groups, holder.includeGroups)
          .add(FillOpts.Applications, holder.includeApplications)
          .add(FillOpts.Environments, holder.includeEnvironments), current.id!!.id
      ) ?: throw NotFoundException()
    }

    throw ForbiddenException("Not an admin, cannot rename portfolio")
  }

  @Deprecated("Deprecated in Java")
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
      groupApi.updateAdminGroupForPortfolio(pf.id, portfolioUtils.formatPortfolioAdminGroupName(pf.name))
      return portfolioApi.getPortfolio(
        pf.id, Opts().add(FillOpts.Groups, holder.includeGroups)
          .add(FillOpts.Applications, holder.includeApplications)
          .add(FillOpts.Environments, holder.includeEnvironments), current.id!!.id
      ) ?: throw NotFoundException()
    }
    throw ForbiddenException("Not an admin, cannot rename portfolio")
  }

  companion object {
    private val log = LoggerFactory.getLogger(PortfolioResource::class.java)
  }
}
