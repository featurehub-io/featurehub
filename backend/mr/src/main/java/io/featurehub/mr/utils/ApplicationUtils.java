package io.featurehub.mr.utils;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.Opts;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Person;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.SecurityContext;
import org.h2.server.web.WebApp;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ApplicationUtils {
  private static final Logger log = LoggerFactory.getLogger(ApplicationUtils.class);
  @NotNull
  private final AuthManagerService authManager;
  @NotNull
  private final ApplicationApi applicationApi;

  @Inject
  public ApplicationUtils(@NotNull AuthManagerService authManager, @NotNull ApplicationApi applicationApi) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
  }

  public ApplicationPermissionCheck check(@NotNull SecurityContext securityContext, @NotNull UUID id) {
    return check(securityContext, id, Opts.empty());
  }

  public ApplicationPermissionCheck check(@NotNull SecurityContext securityContext, @NotNull UUID id, @NotNull Opts opts) {
    Person current = authManager.from(securityContext);

    return check(current, id, opts);
  }

  public ApplicationPermissionCheck check(@NotNull Person current, @NotNull UUID id, @NotNull Opts opts) throws WebApplicationException {

    Application app = applicationApi.getApplication(id, opts);

    if (app == null) {
      throw new NotFoundException();
    }

    if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(app.getPortfolioId(), current, null)) {
      return new ApplicationPermissionCheck(current, app);
    } else {
      throw new ForbiddenException();
    }
  }

  @NotNull public ApplicationPermissionCheck featureCreatorCheck(@NotNull SecurityContext securityContext,
                                                                 @NotNull UUID appId) throws WebApplicationException {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureCreator(appId, current.getId().getId())) {
      log.warn("Attempt by person {} to edit features in application {}", current.getId().getId(), appId);

      return check(current, appId, Opts.empty());
    } else {
      return new ApplicationPermissionCheck(current, new Application().id(appId));
    }
  }

  /**
   * This just checks to see if a person has the Editor/Delete permission and if not, throws an exception. A portfolio
   * admin or admin will always have it.
   *
   * @param securityContext
   * @param appId
   */
  public void featureEditorCheck(@NotNull SecurityContext securityContext, @NotNull UUID appId) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureEditor(appId, current.getId().getId())) {
      log.warn("Attempt by person {} to edt features in application {}", current.getId().getId(), appId);

      check(current, appId, Opts.empty());
    }
  }

  public void featureReadCheck(@NotNull SecurityContext securityContext, @NotNull UUID id) {
    Person current = authManager.from(securityContext);

    if (!applicationApi.personIsFeatureReader(id, current.getId().getId())) {
      throw new ForbiddenException();
    }
  }
}
