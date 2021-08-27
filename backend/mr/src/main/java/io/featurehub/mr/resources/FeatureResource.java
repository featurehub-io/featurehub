package io.featurehub.mr.resources;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.RolloutStrategyValidator;
import io.featurehub.mr.api.FeatureServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.ApplicationFeatureValues;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.utils.ApplicationPermissionCheck;
import io.featurehub.mr.utils.ApplicationUtils;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FeatureResource implements FeatureServiceDelegate {
  private static final Logger log = LoggerFactory.getLogger(FeatureResource.class);
  private final AuthManagerService authManager;
  private final ApplicationApi applicationApi;
  private final ApplicationUtils applicationUtils;
  private final FeatureApi featureApi;

  @Inject
  public FeatureResource(AuthManagerService authManager, EnvironmentApi environmentApi, ApplicationApi applicationApi, ApplicationUtils applicationUtils, FeatureApi featureApi) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
    this.applicationUtils = applicationUtils;
    this.featureApi = featureApi;
  }

  @Override
  public List<Feature> createFeaturesForApplication(UUID id, Feature feature, SecurityContext securityContext) {
    // here we are only calling it to ensure the security check happens
    final ApplicationPermissionCheck appFeaturePermCheck = applicationUtils.featureAdminCheck(securityContext, id);

    try {
      return applicationApi.createApplicationFeature(id, feature, appFeaturePermCheck.getCurrent());
    } catch (ApplicationApi.DuplicateFeatureException e) {
      throw new WebApplicationException(409);
    }
  }

  @Override
  public List<Feature> deleteFeatureForApplication(UUID id, String featureKey, SecurityContext securityContext) {
    // here we are only calling it to ensure the security check happens
    applicationUtils.featureAdminCheck(securityContext, id);

    List<Feature> features = applicationApi.deleteApplicationFeature(id, featureKey);
    if (features == null) {
      throw new NotFoundException();
    }

    return features;
  }

  @Override
  public ApplicationFeatureValues findAllFeatureAndFeatureValuesForEnvironmentsByApplication(UUID id,
                                                                                             SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    final ApplicationFeatureValues allFeatureAndFeatureValuesForEnvironmentsByApplication = featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(id, current);

    if (allFeatureAndFeatureValuesForEnvironmentsByApplication == null) {
      throw new NotFoundException();
    }

    return allFeatureAndFeatureValuesForEnvironmentsByApplication;
  }

  @Override
  public List<FeatureEnvironment> getAllFeatureValuesByApplicationForKey(UUID id, String key,
                                                                         SecurityContext securityContext) {
    final List<FeatureEnvironment> result = featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, authManager.from(securityContext));

    if (result == null) {
      throw new NotFoundException();
    }

    return result;
  }

  @Override
  public List<Feature> getAllFeaturesForApplication(UUID id, SecurityContext securityContext) {
    applicationUtils.featureReadCheck(securityContext, id);
    return applicationApi.getApplicationFeatures(id);
  }

  @Override
  public Feature getFeatureByKey(UUID id, String key, SecurityContext securityContext) {
    // TODO: permission to read the features
    Feature feature = applicationApi.getApplicationFeatureByKey(id, key);

    if (feature == null) {
      throw new NotFoundException();
    }

    return feature;
  }

  @Override
  public List<FeatureEnvironment> updateAllFeatureValuesByApplicationForKey(UUID id, String key,
                                                                            List<FeatureValue> featureValue, UpdateAllFeatureValuesByApplicationForKeyHolder holder, SecurityContext securityContext) {
    final Person person = authManager.from(securityContext);

    try {
      featureApi.updateAllFeatureValuesByApplicationForKey(id, key, featureValue, person, Boolean.TRUE.equals(holder.removeValuesNotPassed));
    } catch (OptimisticLockingException e) {
      log.warn("Optimistic locking failure", e);
      throw new WebApplicationException(409);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      log.warn("User attempted to update feature they had no access to", noAppropriateRole);
      throw new BadRequestException(noAppropriateRole);
    } catch (RolloutStrategyValidator.InvalidStrategyCombination bad) {
      throw new WebApplicationException(Response.status(422).entity(bad.failure).build()); // can't do anything with it
    }

    final List<FeatureEnvironment> featureValues =
      featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, person);

    if (featureValues == null) {
      return new ArrayList<>();
    }

    return featureValues;
  }

  @Override
  public List<Feature> updateFeatureForApplication(UUID id, String key, Feature feature,
                                                   SecurityContext securityContext) {
    applicationUtils.check(securityContext, id);
    try {
      List<Feature> features = applicationApi.updateApplicationFeature(id, key, feature);

      if (features == null) {
        throw new NotFoundException("no such feature name");
      }

      return features;
    } catch (ApplicationApi.DuplicateFeatureException e) {
      throw new WebApplicationException(Response.Status.CONFLICT);
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(422);
    }
  }
}
