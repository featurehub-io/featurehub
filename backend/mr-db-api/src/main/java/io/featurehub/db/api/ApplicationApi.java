package io.featurehub.db.api;

import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;

import java.util.List;
import java.util.Set;

public interface ApplicationApi {

  class DuplicateApplicationException extends Exception {}
  class DuplicateFeatureException extends Exception {}

  Application createApplication(@NotNull String portfolioId, @NotNull Application application, @NotNull Person current) throws DuplicateApplicationException;

  List<Application> findApplications(@NotNull String portfolioId, String filter, SortOrder order, @NotNull Opts opts, @NotNull Person current, boolean loadAll);

  boolean deleteApplication(@NotNull String portfolioId, String applicationId);

  Application getApplication(@NotNull String appId, @NotNull Opts opts);

  Application updateApplication(@NotNull String appId, @NotNull Application application, @NotNull Opts opts) throws DuplicateApplicationException, OptimisticLockingException;

  List<Feature> createApplicationFeature(@NotNull String appId, Feature feature, Person person) throws DuplicateFeatureException;
  List<Feature> updateApplicationFeature(@NotNull String appId, String key, Feature feature) throws DuplicateFeatureException, OptimisticLockingException;
  List<Feature> getApplicationFeatures(@NotNull String appId);
  List<Feature> deleteApplicationFeature(@NotNull String appId, String key);
  Feature getApplicationFeatureByKey(@NotNull String appId, String key);

  Set<String> findFeatureEditors(String id);
  Set<String> findFeatureReaders(String id);
  boolean personIsFeatureReader(String appId, String personId);
}
