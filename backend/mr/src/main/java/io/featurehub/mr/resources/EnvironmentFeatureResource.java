package io.featurehub.mr.resources;

import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.EnvironmentRoles;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.PersonFeaturePermission;
import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.api.EnvironmentFeatureServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.EnvironmentFeaturesResult;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

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

  private PersonFeaturePermission requireRoleCheck(UUID eid, SecurityContext ctx) {
    Person current = authManagerService.from(ctx);

    final EnvironmentRoles roles = environmentApi.personRoles(current, eid);

    return new PersonFeaturePermission.Builder().person(current).appRoles(roles.applicationRoles).roles(roles.environmentRoles).build();
  }

  @Override
  public FeatureValue createFeatureForEnvironment(UUID eid, String key, FeatureValue featureValue,
                                                  SecurityContext securityContext) {
    final FeatureValue featureForEnvironment;

    try {
      featureForEnvironment = featureApi.createFeatureValueForEnvironment(eid, key, featureValue, requireRoleCheck(eid, securityContext));
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(409);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new ForbiddenException(noAppropriateRole);
    } catch (RolloutStrategyValidator.InvalidStrategyCombination bad) {
      throw new WebApplicationException(Response.status(422).entity(bad.failure).build()); // can't do anything with it
    }

    if (featureForEnvironment == null) {
      throw new NotFoundException();
    }

    return featureForEnvironment;
  }

  @Override
  public void deleteFeatureForEnvironment(UUID eid, String key, SecurityContext securityContext) {
    if (!requireRoleCheck(eid, securityContext).hasChangeValueRole()) {
      throw new ForbiddenException();
    }

    if (!featureApi.deleteFeatureValueForEnvironment(eid, key)) {
      throw new NotFoundException();
    }
  }

  @Override
  public FeatureValue getFeatureForEnvironment(UUID eid, String key, SecurityContext securityContext) {
    if (requireRoleCheck(eid, securityContext).hasNoRoles()) {
      throw new ForbiddenException();
    }

    final FeatureValue featureForEnvironment = featureApi.getFeatureValueForEnvironment(eid, key);

    if (featureForEnvironment == null) {
      throw new NotFoundException();
    }

    return featureForEnvironment;
  }

  @Override
  public EnvironmentFeaturesResult getFeaturesForEnvironment(UUID eid, SecurityContext securityContext) {
    if (requireRoleCheck(eid, securityContext).hasNoRoles()) {
      throw new ForbiddenException();
    }

    final EnvironmentFeaturesResult allFeaturesForEnvironment = featureApi.getAllFeatureValuesForEnvironment(eid);

    if (allFeaturesForEnvironment == null) {
      throw new BadRequestException("Not a valid environment id");
    }

    return allFeaturesForEnvironment;
  }

  @Override
  public List<FeatureValue> updateAllFeaturesForEnvironment(UUID eid, List<FeatureValue> featureValues,
                                                            SecurityContext securityContext) {
    List<FeatureValue> updated;

    try {
      updated = featureApi.updateAllFeatureValuesForEnvironment(eid, featureValues,
        requireRoleCheck(eid, securityContext));
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(409);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new ForbiddenException(noAppropriateRole);
    } catch (RolloutStrategyValidator.InvalidStrategyCombination bad) {
      throw new WebApplicationException(Response.status(422).entity(bad.failure).build()); // can't do anything with it
    }

    if (updated == null) {
      throw new NotFoundException();
    }

    return updated;
  }

  @Override
  public FeatureValue updateFeatureForEnvironment(UUID eid, String key, FeatureValue featureValue,
                                                  SecurityContext securityContext) {
    try {
      return featureApi.updateFeatureValueForEnvironment(eid, key, featureValue, requireRoleCheck(eid, securityContext));
    } catch (OptimisticLockingException e) {
      log.error("optimistic locking", e);
      throw new WebApplicationException(409);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new ForbiddenException(noAppropriateRole);
    } catch (RolloutStrategyValidator.InvalidStrategyCombination bad) {
      throw new WebApplicationException(Response.status(422).entity(bad.failure).build()); // can't do anything with it
    }
  }
}
