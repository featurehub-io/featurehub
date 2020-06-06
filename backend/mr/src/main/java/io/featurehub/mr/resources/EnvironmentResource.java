package io.featurehub.mr.resources;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.mr.api.EnvironmentServiceDelegate;
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

public class EnvironmentResource implements EnvironmentServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(EnvironmentResource.class);
  private final AuthManagerService authManager;
  private final EnvironmentApi environmentApi;
  private final ApplicationApi applicationApi;
  private final ApplicationUtils applicationUtils;

  @Inject
  public EnvironmentResource(AuthManagerService authManager, EnvironmentApi environmentApi, ApplicationApi applicationApi, ApplicationUtils applicationUtils) {
    this.authManager = authManager;
    this.environmentApi = environmentApi;
    this.applicationApi = applicationApi;
    this.applicationUtils = applicationUtils;
  }

  @Override
  public Environment createEnvironment(String id, Environment environment, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    boolean hasPermission = authManager.isOrgAdmin(current);
    if (!hasPermission) {
      final Application application = applicationApi.getApplication(id, Opts.empty());
      if (application == null) {
        throw new NotFoundException();
      }

      hasPermission = authManager.isPortfolioAdmin(application.getPortfolioId(), current, null);
    }

    if (hasPermission) {
      try {
        return environmentApi.create(environment, new Application().id(id), current);
      } catch (EnvironmentApi.DuplicateEnvironmentException e) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      } catch (EnvironmentApi.InvalidEnvironmentChangeException e) {
        throw new BadRequestException();
      }
    }

    throw new ForbiddenException();
  }

  @Override
  public Boolean deleteEnvironment(String eid, DeleteEnvironmentHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isOrgAdmin(current) ||
      authManager.isPortfolioAdmin(environmentApi.findPortfolio(eid), current)) {
      return environmentApi.delete(eid);
    }

    return false;
  }

  @Override
  public List<Environment> environmentOrdering(String id, List<Environment> environments, SecurityContext securityContext) {
    final ApplicationPermissionCheck perm = applicationUtils.check(securityContext, id);

    List<Environment> updatedEnvironments = environmentApi.setOrdering(perm.getApp(), environments);

    if (updatedEnvironments == null) {
      throw new BadRequestException();
    }

    return updatedEnvironments;
  }

  @Override
  public List<Environment> findEnvironments(String id, FindEnvironmentsHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    return environmentApi.search(id, holder.filter, holder.order,
      new Opts().add(FillOpts.Acls, holder.includeAcls).add(FillOpts.Features, holder.includeFeatures), current);

  }

  @Override
  public Environment getEnvironment(String eid, GetEnvironmentHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    Environment found = environmentApi.get(eid, new Opts()
        .add(FillOpts.Acls, holder.includeAcls)
        .add(FillOpts.Features, holder.includeFeatures)
        .add(FillOpts.ServiceAccounts, holder.includeServiceAccounts)
        .add(FillOpts.SdkURL, holder.includeSdkUrl)
      , current);

    if (found == null) {
      log.warn("User had no access to environment `{}` or it didn't exist.", eid);
      throw new ForbiddenException();
    }

    return found;
  }

  @Override
  public Environment updateEnvironment(String eid, Environment environment, UpdateEnvironmentHolder holder, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isOrgAdmin(current) ||
      authManager.isPortfolioAdmin(environmentApi.findPortfolio(environment.getId()), current)) {
      try {
        return environmentApi.update(eid, environment, new Opts().add(FillOpts.Acls, holder.includeAcls).add(FillOpts.Features, holder.includeFeatures));
      } catch (OptimisticLockingException e) {
        throw new WebApplicationException(422);
      } catch (EnvironmentApi.DuplicateEnvironmentException e) {
        throw new WebApplicationException(Response.Status.CONFLICT);
      } catch (EnvironmentApi.InvalidEnvironmentChangeException e) {
        throw new BadRequestException();
      }
    }

    return null;
  }
}
