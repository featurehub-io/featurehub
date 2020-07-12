package io.featurehub.mr.utils;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.Opts;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.SecurityContext;

public class ApplicationUtils {
  private static final Logger log = LoggerFactory.getLogger(ApplicationUtils.class);
  private final AuthManagerService authManager;
  private final ApplicationApi applicationApi;

  @Inject
  public ApplicationUtils(AuthManagerService authManager, ApplicationApi applicationApi) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
  }

  public ApplicationPermissionCheck check(SecurityContext securityContext, String id) {
    return check(securityContext, id, Opts.empty());
  }

  public ApplicationPermissionCheck check(SecurityContext securityContext, String id, Opts opts) {
    Person current = authManager.from(securityContext);

    return check(current, id, opts);
  }

  public ApplicationPermissionCheck check(Person current, String id, Opts opts) {

    Application app = applicationApi.getApplication(id, opts);

    if (app == null) {
      throw new NotFoundException();
    }

    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(app.getPortfolioId(), current, null)) {
      return new ApplicationPermissionCheck.Builder().app(app).current(current).build();
    } else {
      throw new ForbiddenException();
    }
  }

  public ApplicationPermissionCheck featureCheck(SecurityContext securityContext, String id) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.findFeatureEditors(id).contains(current.getId().getId())) {
      log.warn("Attempt by person {} to edt features in application {}", current.getId().getId(), id);

      return check(current, id, Opts.empty());
    } else {
      return new ApplicationPermissionCheck.Builder().current(current).build();
    }
  }

  public void featureReadCheck(SecurityContext securityContext, String id) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureReader(id, current.getId().getId())) {
      throw new ForbiddenException();
    }
  }
}
