package io.featurehub.mr.resources;

import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.EnvironmentRoles;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.PersonFeaturePermission;
import io.featurehub.mr.api.EnvironmentFeatureServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.EnvironmentFeaturesResult;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.RoleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

public class EnvironmentFeatureResource implements EnvironmentFeatureServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(EnvironmentFeatureResource.class);
  private final EnvironmentApi environmentApi;
  private final AuthManagerService authManagerService;
  private final FeatureApi featureApi;

  @Inject
  public EnvironmentFeatureResource(EnvironmentApi environmentApi, AuthManagerService authManagerService, FeatureApi featureApi) {
    this.environmentApi = environmentApi;
    this.authManagerService = authManagerService;
    this.featureApi = featureApi;
  }

  private PersonFeaturePermission requireRoleCheck(String eid, SecurityContext ctx) {
    Person current = authManagerService.from(ctx);

    final EnvironmentRoles roles = environmentApi.personRoles(current, eid);

    return new PersonFeaturePermission.Builder().person(current).appRoles(roles.applicationRoles).roles(roles.environmentRoles).build();
  }

  @Override
  public FeatureValue createFeatureForEnvironment(String eid, String key, FeatureValue featureValue, SecurityContext securityContext) {
    final FeatureValue featureForEnvironment;

    try {
      featureForEnvironment = featureApi.createFeatureValueForEnvironment(eid, key, featureValue, requireRoleCheck(eid, securityContext));
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(422);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new ForbiddenException(noAppropriateRole);
    }

    if (featureForEnvironment == null) {
      throw new NotFoundException();
    }

    return featureForEnvironment;
  }

  @Override
  public void deleteFeatureForEnvironment(String eid, String key, SecurityContext securityContext) {
    if (!requireRoleCheck(eid, securityContext).hasEditRole()) {
      throw new ForbiddenException();
    }

    if (!featureApi.deleteFeatureValueForEnvironment(eid, key)) {
      throw new NotFoundException();
    }
  }

  @Override
  public FeatureValue getFeatureForEnvironment(String eid, String key, SecurityContext securityContext) {
    if (!requireRoleCheck(eid, securityContext).hasReadRole()) {
      throw new ForbiddenException();
    }

    final FeatureValue featureForEnvironment = featureApi.getFeatureValueForEnvironment(eid, key);

    if (featureForEnvironment == null) {
      throw new NotFoundException();
    }

    return featureForEnvironment;
  }

  @Override
  public EnvironmentFeaturesResult getFeaturesForEnvironment(String eid, SecurityContext securityContext) {
    if ("latest".equalsIgnoreCase(eid)) {
      return featureApi.lastFeatureValueChanges(authManagerService.from(securityContext));
    }

    if (!requireRoleCheck(eid, securityContext).hasReadRole()) {
      throw new ForbiddenException();
    }

    final EnvironmentFeaturesResult allFeaturesForEnvironment = featureApi.getAllFeatureValuesForEnvironment(eid);

    if (allFeaturesForEnvironment == null) {
      throw new BadRequestException("Not a valid environment id");
    }

    return allFeaturesForEnvironment;
  }

  @Override
  public List<FeatureValue> updateAllFeaturesForEnvironment(String eid, List<FeatureValue> featureValues, SecurityContext securityContext) {
    try {
      return featureApi.updateAllFeatureValuesForEnvironment(eid, featureValues, requireRoleCheck(eid, securityContext));
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(422);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new ForbiddenException(noAppropriateRole);
    }
  }

  @Override
  public FeatureValue updateFeatureForEnvironment(String eid, String key, FeatureValue featureValue, SecurityContext securityContext) {
    try {
      return featureApi.updateFeatureValueForEnvironment(eid, key, featureValue, requireRoleCheck(eid, securityContext));
    } catch (OptimisticLockingException e) {
      log.error("optimistic locking", e);
      throw new WebApplicationException(422);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new ForbiddenException(noAppropriateRole);
    }
  }
}
