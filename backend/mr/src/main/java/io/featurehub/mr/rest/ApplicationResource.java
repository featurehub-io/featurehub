package io.featurehub.mr.rest;

import io.featurehub.db.api.ApplicationApi;
import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FeatureApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.OptimisticLockingException;
import io.featurehub.db.api.Opts;
import io.featurehub.mr.api.ApplicationSecuredService;
import io.featurehub.mr.auth.AuthManagerService;
import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.ApplicationFeatureValues;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureEnvironment;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

@Singleton
public class ApplicationResource implements ApplicationSecuredService {
  private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
  private final AuthManagerService authManager;
  private final ApplicationApi applicationApi;
  private final EnvironmentApi environmentApi;
  private final FeatureApi featureApi;

  @Inject
  public ApplicationResource(AuthManagerService authManager, ApplicationApi applicationApi, EnvironmentApi environmentApi, FeatureApi featureApi) {
    this.authManager = authManager;
    this.applicationApi = applicationApi;
    this.environmentApi = environmentApi;
    this.featureApi = featureApi;
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

  class ApplicationPermissionCheck {
    Person current;
    Application app;

    ApplicationPermissionCheck(SecurityContext securityContext, String id) {
      this(securityContext, id, Opts.empty());
    }

    ApplicationPermissionCheck(SecurityContext securityContext, String id, Opts opts) {
      permCheckAdmin(securityContext, id, opts);
    }

    protected void permCheckAdmin(SecurityContext securityContext, String id, Opts opts) {
      Person current = authManager.from(securityContext);

      Application app = applicationApi.getApplication(id, opts);

      if (app == null) {
        throw new NotFoundException();
      }

      if (authManager.isOrgAdmin(current) || authManager.isPortfolioAdmin(app.getPortfolioId(), current, null)) {
        this.current = current;
        this.app = app;
      } else {
        throw new ForbiddenException();
      }
    }
  }

  class ApplicationFeaturePermissionCheck {
    public ApplicationFeaturePermissionCheck(SecurityContext securityContext, String id) {
      Person current = authManager.from(securityContext);

      if (!applicationApi.findFeatureEditors(id).contains(current.getId().getId())) {
        log.warn("Attempt by {} to edt features in application {}", current.getEmail(), id);

        new ApplicationPermissionCheck(securityContext, id);
      }
    }
  }

  @Override
  public List<Feature> createFeaturesForApplication(String id, Feature feature, SecurityContext securityContext) {
    // here we are only calling it to ensure the security check happens
    new ApplicationFeaturePermissionCheck(securityContext, id);

    try {
      return applicationApi.createApplicationFeature(id, feature);
    } catch (ApplicationApi.DuplicateFeatureException e) {
      throw new WebApplicationException(409);
    }
  }

  @Override
  public Boolean deleteApplication(String id, Boolean includeEnvionments, SecurityContext securityContext) {

    ApplicationPermissionCheck apc = new ApplicationPermissionCheck(securityContext, id);

    return applicationApi.deleteApplication(apc.app.getPortfolioId(), apc.app.getId());
  }

  @Override
  public List<Feature> deleteFeatureForApplication(String id, String key, SecurityContext securityContext) {
    // here we are only calling it to ensure the security check happens
    new ApplicationFeaturePermissionCheck(securityContext, id);

    List<Feature> features = applicationApi.deleteApplicationFeature(id, key);
    if (features == null) {
      throw new NotFoundException();
    }

    return features;
  }

  @Override
  public List<Environment> environmentOrdering(String id, List<Environment> environments, SecurityContext securityContext) {
    final ApplicationPermissionCheck perm = new ApplicationPermissionCheck(securityContext, id);

    List<Environment> updatedEnvironments = environmentApi.setOrdering(perm.app, environments);

    if (updatedEnvironments == null) {
      throw new BadRequestException();
    }

    return updatedEnvironments;
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
  public List<Environment> findEnvironments(String id, SortOrder order, String filter, Boolean includeAcls, Boolean includeFeatures, SecurityContext securityContext) {
    Person current = authManager.from(securityContext);

    return environmentApi.search(id, filter, order, new Opts().add(FillOpts.Acls, includeAcls).add(FillOpts.Features, includeFeatures), current);
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
    ApplicationPermissionCheck apc = new ApplicationPermissionCheck(securityContext, id);

    return applicationApi.getApplicationFeatures(id);
  }

  @Override
  public Application getApplication(String appId, Boolean includeEnvionments, SecurityContext securityContext) {
//    Person current = authManager.from(securityContext);

    Application app = applicationApi.getApplication(appId, new Opts().add(FillOpts.Environments, includeEnvionments));

    return app;
  }

  @Override
  public Feature getFeatureByKey(String id, String key, SecurityContext securityContext) {
    // TODO: permission
    Feature feature = applicationApi.getApplicationFeatureByKey(id, key);

    if (feature == null) {
      throw new NotFoundException();
    }

    return feature;
  }

  @Override
  public List<FeatureEnvironment> updateAllFeatureValuesByApplicationForKey(String id, String key, List<FeatureValue> featureValue, Boolean removeValuesNotPassed, SecurityContext securityContext) {
    final Person person = authManager.from(securityContext);

    try {
      featureApi.updateAllFeatureValuesByApplicationForKey(id, key, featureValue, person, Boolean.TRUE.equals(removeValuesNotPassed));
    } catch (OptimisticLockingException e) {
      e.printStackTrace();
      throw new WebApplicationException(422);
    } catch (FeatureApi.NoAppropriateRole noAppropriateRole) {
      throw new BadRequestException(noAppropriateRole);
    }

    return featureApi.getFeatureValuesForApplicationForKeyForPerson(id, key, person);
  }

  @Override
  public Application updateApplication(String id, Application application, Boolean includeEnvionments, SecurityContext securityContext) {
    new ApplicationPermissionCheck(securityContext, id);

    try {
      return applicationApi.updateApplication(id, application, new Opts().add(FillOpts.Environments, includeEnvionments));
    } catch (ApplicationApi.DuplicateApplicationException e) {
      throw new WebApplicationException(Response.Status.CONFLICT);
    } catch (OptimisticLockingException e) {
      throw new WebApplicationException(422);
    }
  }

  @Override
  public List<Feature> updateFeatureForApplication(String id, String key, Feature feature, SecurityContext securityContext) {
    new ApplicationFeaturePermissionCheck(securityContext, id);

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
