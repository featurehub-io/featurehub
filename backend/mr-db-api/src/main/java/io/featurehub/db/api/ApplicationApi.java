package io.featurehub.db.api;

import io.featurehub.mr.model.Application;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ApplicationApi {

  class DuplicateApplicationException extends Exception {}
  class DuplicateFeatureException extends Exception {}

  Application createApplication(@NotNull UUID portfolioId, @NotNull Application application, @NotNull Person current) throws DuplicateApplicationException;

  List<Application> findApplications(@NotNull UUID portfolioId, String filter, SortOrder order, @NotNull Opts opts, @NotNull Person current, boolean loadAll);

  boolean deleteApplication(@NotNull UUID portfolioId, UUID applicationId);

  Application getApplication(@NotNull UUID appId, @NotNull Opts opts);

  Application updateApplication(@NotNull UUID appId, @NotNull Application application, @NotNull Opts opts) throws DuplicateApplicationException, OptimisticLockingException;

  List<Feature> createApplicationFeature(@NotNull UUID appId, Feature feature, Person person) throws DuplicateFeatureException;
  List<Feature> updateApplicationFeature(@NotNull UUID appId, String key, Feature feature) throws DuplicateFeatureException, OptimisticLockingException;
  List<Feature> getApplicationFeatures(@NotNull UUID appId);
  List<Feature> deleteApplicationFeature(@NotNull UUID appId, String key);
  Feature getApplicationFeatureByKey(@NotNull UUID appId, String key);

  Set<UUID> findFeatureEditors(UUID id);
  Set<UUID> findFeatureReaders(UUID id);
  boolean personIsFeatureReader(UUID appId, UUID personId);
}
