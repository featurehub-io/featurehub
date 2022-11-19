package io.featurehub.mr.utils;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.Opts;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Person;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ApplicationUtils {
  private static final Logger log = LoggerFactory.getLogger(ApplicationUtils.class);
  private final AuthManagerService authManager;
  private final ApplicationApi applicationApi;

  @Inject
  public ApplicationUtils(AuthManagerService authManager, ApplicationApi applicationApi) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
  }

  public ApplicationPermissionCheck check(SecurityContext securityContext, UUID id) {
    return check(securityContext, id, Opts.empty());
  }

  public ApplicationPermissionCheck check(SecurityContext securityContext, UUID id, Opts opts) {
    Person current = authManager.from(securityContext);

    return check(current, id, opts);
  }

  public ApplicationPermissionCheck check(Person current, UUID id, Opts opts) {

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

  public ApplicationPermissionCheck featureCreatorCheck(SecurityContext securityContext, UUID appId) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureCreator(appId, current.getId().getId())) {
      log.warn("Attempt by person {} to edt features in application {}", current.getId().getId(), appId);

      return check(current, appId, Opts.empty());
    } else {
      return new ApplicationPermissionCheck.Builder().current(current).build();
    }
  }

  /**
   * This just checks to see if a person has the Editor/Delete permission and if not, throws an exception. A portfolio
   * admin or admin will always have it.
   *
   * @param securityContext
   * @param appId
   */
  public void featureEditorCheck(SecurityContext securityContext, UUID appId) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureEditor(appId, current.getId().getId())) {
      log.warn("Attempt by person {} to edt features in application {}", current.getId().getId(), appId);

      check(current, appId, Opts.empty());
    }
  }

  public void featureReadCheck(SecurityContext securityContext, UUID id) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureReader(id, current.getId().getId())) {
      throw new ForbiddenException();
    }
  }
}
