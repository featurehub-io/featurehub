package io.featurehub.mr.resources;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.mr.api.PortfolioServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.ApplicationGroupRole;
import io.featurehub.mr.model.ApplicationRoleType;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.utils.PortfolioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PortfolioResource implements PortfolioServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(PortfolioResource.class);

  private final GroupApi groupApi;
  private final AuthManagerService authManager;
  private final PortfolioApi portfolioApi;
  private final OrganizationApi organizationApi;
  private final PortfolioUtils portfolioUtils;

  @Inject
  public PortfolioResource(GroupApi groupApi, AuthManagerService authManager, PortfolioApi portfolioApi, OrganizationApi organizationApi, PortfolioUtils portfolioUtils) {
    this.groupApi = groupApi;
    this.authManager = authManager;
    this.portfolioApi = portfolioApi;
    this.organizationApi = organizationApi;
    this.portfolioUtils = portfolioUtils;
  }

  @Override
  public Portfolio createPortfolio(Portfolio portfolio, CreatePortfolioHolder holder, SecurityContext securityContext) {
    if (authManager.isOrgAdmin(authManager.from(securityContext))) {
      Opts opts = new Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.Applications, holder.includeApplications);

      Portfolio created;
      try {
        created = portfolioApi.createPortfolio(portfolio, opts, authManager.from(securityContext));
      } catch (PortfolioApi.DuplicatePortfolioException e) {
        log.error("Duplicate portfolio name", e);
        throw new WebApplicationException(Response.Status.CONFLICT);
      }

      if (created == null) {
        throw new NotFoundException("No conditions to allow you to create.");
      }

      Group group;
      try {
        group = groupApi.createPortfolioGroup(created.getId(),
          new Group()
            .name(portfolioUtils.formatPortfolioAdminGroupName(portfolio))
            .admin(true)
            .portfolioId(created.getId()),
          authManager.from(securityContext));
      } catch (GroupApi.DuplicateGroupException e) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      }

      created.addGroupsItem(group);

      return created;
    }

    throw new ForbiddenException("No permission to create portfolio");
  }

  @Override
  public Boolean deletePortfolio(String id, DeletePortfolioHolder holder, SecurityContext securityContext) {
    final Person from = authManager.from(securityContext);
    if (authManager.isOrgAdmin(from)) {
      if (portfolioApi.getPortfolio(id, Opts.empty(), from) != null) {
        portfolioApi.deletePortfolio(id);
        return true;
      } else {
        return false; // no portfolio
      }
    }

    throw new ForbiddenException("Not allowed to delete portfolio");
  }

  @Override
  public List<Portfolio> findPortfolios(FindPortfoliosHolder holder, SecurityContext securityContext) {
    return portfolioApi.findPortfolios(holder.filter,
      authManager.orgPersonIn(
        authManager.from(securityContext)
      ), holder.order,
      new Opts().add(FillOpts.Groups, holder.includeGroups).add(FillOpts.Applications, holder.includeApplications),
      authManager.from(securityContext));
  }

  @Override
  public Portfolio getPortfolio(String id, GetPortfolioHolder holder, SecurityContext securityContext) {
    Portfolio portfolio = portfolioApi.getPortfolio(id,
      new Opts().add(FillOpts.Groups, holder.includeGroups)
        .add(FillOpts.Applications, holder.includeApplications)
        .add(FillOpts.Environments, holder.includeEnvironments)
      , authManager.from(securityContext));
    if (portfolio == null) {
      throw new NotFoundException("No such portfolio");
    }

    return portfolio;
  }

  @Override
  public Portfolio updatePortfolio(String id, Portfolio portfolio, UpdatePortfolioHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, current, null)) {
      portfolio.setId(id);
      Portfolio pf;

      try {
        pf = portfolioApi.updatePortfolio(portfolio, Opts.empty());
      } catch (PortfolioApi.DuplicatePortfolioException e) {
        log.error("Duplicate portfolio name", e);
        throw new WebApplicationException(Response.Status.CONFLICT);
      } catch (OptimisticLockingException e) {
        throw new WebApplicationException(422);
      }

      if (pf == null) {
        throw new NotFoundException("No such portfolio");
      }

      groupApi.updateAdminGroupForPortfolio(pf.getId(), portfolioUtils.formatPortfolioAdminGroupName(pf));

      return portfolioApi.getPortfolio(pf.getId(), new Opts().add(FillOpts.Groups, holder.includeGroups)
        .add(FillOpts.Applications, holder.includeApplications)
        .add(FillOpts.Environments, holder.includeEnvironments), current);
    }

    throw new ForbiddenException("Not an admin, cannot rename portfolio");
  }
}
