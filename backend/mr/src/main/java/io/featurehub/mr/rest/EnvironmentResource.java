package io.featurehub.mr.rest;

import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.ServiceAccountApi;
import io.featurehub.mr.api.EnvironmentSecuredService;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class EnvironmentResource implements EnvironmentSecuredService {
  private static final Logger log = LoggerFactory.getLogger(EnvironmentResource.class);
  private final AuthManagerService authManager;
  private final EnvironmentApi environmentApi;
  private final ServiceAccountApi serviceAccountApi;

  @Inject
  public EnvironmentResource(AuthManagerService authManager, EnvironmentApi environmentApi, ServiceAccountApi serviceAccountApi) {
    this.authManager = authManager;
    this.environmentApi = environmentApi;
    this.serviceAccountApi = serviceAccountApi;
  }

  @Override
  public Boolean deleteEnvironment(String eid, Boolean includeAcls, Boolean includeFeatures, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isOrgAdmin(current) ||
      authManager.isPortfolioAdmin(environmentApi.findPortfolio(eid), current)) {
      return environmentApi.delete(eid);
    }

    return false;
  }

  @Override
  public Environment getEnvironment(String eid, Boolean includeAcls, Boolean includeFeatures,Boolean includeSdkUrl, Boolean includeServiceAccounts, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    Environment found = environmentApi.get(eid, new Opts()
        .add(FillOpts.Acls, includeAcls)
        .add(FillOpts.Features, includeFeatures)
        .add(FillOpts.ServiceAccounts, includeServiceAccounts)
        .add(FillOpts.SdkURL, includeSdkUrl)
      , current);

    if (found == null) {
      log.warn("User had no access to environment `{}` or it didn't exist.", eid);
      throw new ForbiddenException();
    }

    return found;
  }

  @Override
  public Environment updateEnvironment(String eid, Environment environment, Boolean includeAcls, Boolean includeFeatures, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    if (authManager.isOrgAdmin(current) ||
      authManager.isPortfolioAdmin(environmentApi.findPortfolio(environment.getId()), current)) {
      try {
        return environmentApi.update(eid, environment, new Opts().add(FillOpts.Acls, includeAcls).add(FillOpts.Features, includeFeatures));
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
