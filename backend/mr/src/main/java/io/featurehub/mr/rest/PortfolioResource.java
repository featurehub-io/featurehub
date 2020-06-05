package io.featurehub.mr.rest;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.mr.api.PortfolioSecuredService;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.SortOrder;
import io.featurehub.mr.utils.PortfolioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Singleton
public class PortfolioResource implements PortfolioSecuredService {
  private static final Logger log = LoggerFactory.getLogger(PortfolioResource.class);
  private final AuthManagerService authManager;
  private final ApplicationApi applicationApi;
  private final GroupApi groupApi;
  private final PortfolioApi portfolioApi;
  private final OrganizationApi organizationApi;
  private final ServiceAccountApi serviceAccountApi;
  private final PortfolioUtils portfolioUtils;
  private final EnvironmentApi environmentApi;

  @ConfigKey("environment.production.name")
  String productionEnvironmentName = "production";
  @ConfigKey("environment.production.desc")
  String productionEnvironmentDescription = "production";

  @Inject
  public PortfolioResource(AuthManagerService authManager, ApplicationApi applicationApi, GroupApi groupApi,
                           PortfolioApi portfolioApi, OrganizationApi organizationApi,
                           ServiceAccountApi serviceAccountApi, PortfolioUtils portfolioUtils, EnvironmentApi environmentApi) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
    this.groupApi = groupApi;
    this.portfolioApi = portfolioApi;
    this.organizationApi = organizationApi;
    this.serviceAccountApi = serviceAccountApi;
    this.portfolioUtils = portfolioUtils;
    this.environmentApi = environmentApi;
    DeclaredConfigResolver.resolve(this);

  }

  @Override
  public Application createApplication(String id, Application application, Boolean includeEnvionments, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(id, current, null)) {
      try {
        Application app = applicationApi.createApplication(id, application, current);
        environmentApi.create(new Environment().applicationId(app.getId()).name(productionEnvironmentName).production(true).description(productionEnvironmentDescription), app, current);
        if (Boolean.TRUE.equals(includeEnvionments)) {
          app = applicationApi.getApplication(app.getId(), Opts.opts(FillOpts.Environments));
        }
        return app;
      } catch (ApplicationApi.DuplicateApplicationException|EnvironmentApi.DuplicateEnvironmentException e) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      } catch (EnvironmentApi.InvalidEnvironmentChangeException e) {
        log.error("Failed to change environment", e);
        throw new BadRequestException();
      }
    }

    throw new ForbiddenException("No permission to add application");
  }

  @Override
  public Group createGroup(String id, Group group, Boolean includePeople, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isPortfolioAdmin(id, current, null)) {
      try {
        return groupApi.createPortfolioGroup(id, group, current);
      } catch (GroupApi.DuplicateGroupException e) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    }

    throw new ForbiddenException("No permission to add application");
  }

  @Override
  public Portfolio createPortfolio(Portfolio portfolio, Boolean includeGroups, Boolean includeApplications, SecurityContext securityContext) {
    if (authManager.isOrgAdmin(authManager.from(securityContext))) {
      Opts opts = getOpts(includeGroups, includeApplications, null);

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
        group = groupApi.createPortfolioGroup(created.getId(), new Group().name(portfolioUtils.formatPortfolioAdminGroupName(portfolio)).admin(true).portfolioId(created.getId()),
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
  public ServiceAccount createServiceAccountInPortfolio(String id, ServiceAccount serviceAccount, Boolean includePermissions, SecurityContext ctx) {
    Person person = authManager.from(ctx);

    if (authManager.isAnyAdmin(person)) {
      try {
        return serviceAccountApi.create(id, person, serviceAccount, new Opts().add(FillOpts.Permissions, includePermissions));
      } catch (ServiceAccountApi.DuplicateServiceAccountException e) {
        log.warn("Attempt to create duplicate service account {}", serviceAccount.getName());
        throw new WebApplicationException(Response.Status.CONFLICT);
      }
    }

    throw new ForbiddenException();
  }

  @Override
  public Boolean deletePortfolio(String id, Boolean includeGroups, Boolean includeApplications, Boolean includeEnvironments, SecurityContext securityContext) {
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
  public List<Application> findApplications(String id, Boolean includeEnvironments, SortOrder order, String filter, SecurityContext securityContext) {
    final Person from = authManager.from(securityContext);

    final List<Application> applications = applicationApi.findApplications(id, filter, order, new Opts().add(FillOpts.Environments, includeEnvironments), from,
      authManager.isOrgAdmin(from) || authManager.isPortfolioAdmin(id, from, null));

    if (applications == null) {
      throw new NotFoundException();
    }

    return applications;
  }

  @Override
  public List<Group> findGroups(String id, Boolean includePeople, SortOrder order, String filter, SecurityContext securityContext) {
    final Person from = authManager.from(securityContext);

    if (authManager.isOrgAdmin(from) || authManager.isPortfolioGroupMember(id, from)) {
      return groupApi.findGroups(id, filter, order, new Opts().add(FillOpts.People, includePeople));
    }

    throw new ForbiddenException();
  }

  @Override
  public List<Portfolio> findPortfolios(Boolean includeGroups, Boolean includeApplications, SortOrder order, String filter, String parentPortfolioId, SecurityContext securityContext) {
    return portfolioApi.findPortfolios(filter, organizationApi.get().getId(), order, getOpts(includeGroups, includeApplications, null), authManager.from(securityContext));
  }

  private Opts getOpts(Boolean includeGroups, Boolean includeApplications, Boolean includeEnvironments) {
    return new Opts()
      .add(FillOpts.Groups, includeGroups)
      .add(FillOpts.Environments, includeEnvironments)
      .add(FillOpts.Applications, includeApplications);
  }

  @Override
  public Portfolio getPortfolio(String id, Boolean includeGroups, Boolean includeApplications, Boolean includeEnvironments, SecurityContext securityContext) {
    Portfolio portfolio = portfolioApi.getPortfolio(id, getOpts(includeGroups, includeApplications, includeEnvironments), authManager.from(securityContext));
    if (portfolio == null) {
      throw new NotFoundException("No such portfolio");
    }

    return portfolio;
  }

  @Override
  public List<ServiceAccount> searchServiceAccountsInPortfolio(String id, Boolean includePermissions, String filter, String applicationId, SecurityContext ctx) {
    Person person = authManager.from(ctx);

    if (authManager.isAnyAdmin(person)) {
      return serviceAccountApi.search(id, filter, applicationId, new Opts().add(FillOpts.Permissions, includePermissions));
    }

    throw new ForbiddenException();
  }

  @Override
  public Portfolio updatePortfolio(String id, Portfolio portfolio, Boolean includeGroups, Boolean includeApplications, Boolean includeEnvironments, SecurityContext securityContext) {
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

      return portfolioApi.getPortfolio(pf.getId(), getOpts(includeGroups, includeApplications, includeEnvironments), current);
    }

    throw new ForbiddenException("Not an admin, cannot rename portfolio");
  }
}
