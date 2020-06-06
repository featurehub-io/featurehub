package io.featurehub.mr.resources;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.mr.api.FeatureServiceDelegate;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.ApplicationFeatureValues;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.utils.ApplicationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

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
  public List<Feature> createFeaturesForApplication(String id, Feature feature, SecurityContext securityContext) {
    // here we are only calling it to ensure the security check happens
    applicationUtils.featureCheck(securityContext, id);

    try {
      return applicationApi.createApplicationFeature(id, feature);
    } catch (ApplicationApi.DuplicateFeatureException e) {
      throw new WebApplicationException(409);
    }
  }

  @Override
  public List<Feature> deleteFeatureForApplication(String id, String key, SecurityContext securityContext) {
    // here we are only calling it to ensure the security check happens
    applicationUtils.featureCheck(securityContext, id);

    List<Feature> features = applicationApi.deleteApplicationFeature(id, key);
    if (features == null) {
      throw new NotFoundException();
    }

    return features;
  }

  @Override
  public ApplicationFeatureValues findAllFeatureAndFeatureValuesForEnvironmentsByApplication(String id, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    final ApplicationFeatureValues allFeatureAndFeatureValuesForEnvironmentsByApplication = featureApi.findAllFeatureAndFeatureValuesForEnvironmentsByApplication(id, current);

    if (allFeatureAndFeatureValuesForEnvironmentsByApplication == null) {
      throw new NotFoundException();
    }

    return allFeatureAndFeatureValuesForEnvironmentsByApplication;
  }

  @Override
  public List<FeatureEnvironment> getAllFeatureValuesByApplicationForKey(String id, String key, SecurityContext securityContext) {
    final List<FeatureEnvironment> result = featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, authManager.from(securityContext));

    if (result == null) {
      throw new NotFoundException();
    }

    return result;
  }

  @Override
  public List<Feature> getAllFeaturesForApplication(String id, SecurityContext securityContext) {
    // TODO: you should be in a group that can at least READ the features.
    applicationUtils.featureCheck(securityContext, id);
    return applicationApi.getApplicationFeatures(id);
  }

  @Override
  public Feature getFeatureByKey(String id, String key, SecurityContext securityContext) {
    // TODO: permission to read the features
    Feature feature = applicationApi.getApplicationFeatureByKey(id, key);

    if (feature == null) {
      throw new NotFoundException();
    }

    return feature;
  }

  @Override
  public List<FeatureEnvironment> updateAllFeatureValuesByApplicationForKey(String id, String key, List<FeatureValue> featureValue, UpdateAllFeatureValuesByApplicationForKeyHolder holder, SecurityContext securityContext) {
    final Person person = authManager.from(securityContext);

    try {
      featureApi.updateAllFeatureValuesByApplicationForKey(id, key, featureValue, person, Boolean.TRUE.equals(holder.removeValuesNotPassed));
    } catch (OptimisticLockingException e) {
      e.printStackTrace();
      throw new WebApplicationException(422);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new BadRequestException(noAppropriateRole);
    }

    return featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, person);
  }

  @Override
  public List<Feature> updateFeatureForApplication(String id, String key, Feature feature, SecurityContext securityContext) {
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
