package io.featurehub.mr.resources;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.mr.api.ApplicationServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.utils.ApplicationPermissionCheck;
import io.featurehub.mr.utils.ApplicationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

public class ApplicationResource implements ApplicationServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
  private final AuthManagerService authManager;
  private final ApplicationApi applicationApi;
  private final EnvironmentApi environmentApi;
  private final ApplicationUtils applicationUtils;

  @ConfigKey("environment.production.name")
  String productionEnvironmentName = "production";
  @ConfigKey("environment.production.desc")
  String productionEnvironmentDescription = "production";

  @Inject
  public ApplicationResource(AuthManagerService authManager, ApplicationApi applicationApi, EnvironmentApi environmentApi, ApplicationUtils applicationUtils) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
    this.environmentApi = environmentApi;
    this.applicationUtils = applicationUtils;

    DeclaredConfigResolver.resolve(this);
  }

  @Override
  public Application createApplication(String id, Application application, CreateApplicationHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(id, current, null)) {
      try {
        Application app = applicationApi.createApplication(id, application, current);
        environmentApi.create(new Environment().applicationId(app.getId()).name(productionEnvironmentName).production(true).description(productionEnvironmentDescription), app, current);
        if (Boolean.TRUE.equals(holder.includeEnvironments)) {
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
  public Boolean deleteApplication(String eid, DeleteApplicationHolder holder, SecurityContext securityContext) {
    ApplicationPermissionCheck apc = applicationUtils.check(securityContext, eid);

    return applicationApi.deleteApplication(apc.getApp().getPortfolioId(), apc.getApp().getId());
  }

  @Override
  public List<Application> findApplications(String id, FindApplicationsHolder holder, SecurityContext securityContext) {
    final Person from = authManager.from(securityContext);

    final List<Application> applications = applicationApi.findApplications(id, holder.filter, holder.order, new Opts().add(FillOpts.Environments, holder.includeEnvironments), from,
      authManager.isOrgAdmin(from) || authManager.isPortfolioAdmin(id, from, null));

    if (applications == null) {
      throw new NotFoundException();
    }

    return applications;
  }

  @Override
  public Application getApplication(String appId, GetApplicationHolder holder, SecurityContext securityContext) {
    return applicationApi.getApplication(appId, new Opts().add(FillOpts.Environments, holder.includeEnvironments));
  }

  @Override
  public Application updateApplication(String appId, Application application, UpdateApplicationHolder holder, SecurityContext securityContext) {
    applicationUtils.check(securityContext, appId);

    try {
      return applicationApi.updateApplication(appId, application, new Opts().add(FillOpts.Environments, holder.includeEnvironments));
    } catch (ApplicationApi.DuplicateApplicationException e) {
      throw new WebApplicationException(Response.Status.CONFLICT);
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(422);
    }
  }
}
