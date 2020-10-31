package io.featurehub.mr.resources;

import io.featurehub.db.api.UserStateApi;
import io.featurehub.mr.api.UserStateServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.HiddenEnvironments;
import io.featurehub.mr.utils.ApplicationUtils;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class UserStateResource implements UserStateServiceDelegate {
  private final AuthManagerService authManager;
  private final ApplicationUtils applicationUtils;
  private final UserStateApi userStateApi;

  @Inject
  public UserStateResource(AuthManagerService authManager, ApplicationUtils applicationUtils, UserStateApi userStateApi) {
    this.authManager = authManager;
    this.applicationUtils = applicationUtils;
    this.userStateApi = userStateApi;
  }

  @Override
  public HiddenEnvironments getHiddenEnvironments(String appId, SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, appId);

    final HiddenEnvironments hiddenEnvironments =
      userStateApi.getHiddenEnvironments(authManager.from(securityContext), appId);

    return hiddenEnvironments == null ? new HiddenEnvironments() : hiddenEnvironments;
  }

  @Override
  public HiddenEnvironments saveHiddenEnvironments(String appId, HiddenEnvironments hiddenEnvironments, SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, appId);

    try {
      userStateApi.saveHiddenEnvironments(authManager.from(securityContext), hiddenEnvironments, appId);
    } catch (UserStateApi.InvalidUserStateException e) {
      throw new WebApplicationException(Response.status(422).entity(e.getMessage()).build());
    }

    return hiddenEnvironments;
  }
}
