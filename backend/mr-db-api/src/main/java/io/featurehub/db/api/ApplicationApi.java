package io.featurehub.db.api;

import io.featurehub.mr.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ApplicationApi {

  @Nullable
  ApplicationSummary getApplicationSummary(@NotNull UUID appId);

  class DuplicateApplicationException extends Exception {}
  class DuplicateFeatureException extends Exception {}

  @Nullable
  Application createApplication(@NotNull UUID portfolioId, @NotNull CreateApplication application, @NotNull Person current) throws DuplicateApplicationException;

  @NotNull
  List<Application> findApplications(@NotNull UUID portfolioId, @Nullable String filter, SortOrder order,
                                     @NotNull Opts opts, @NotNull Person current, boolean loadAll);

  boolean deleteApplication(@NotNull UUID portfolioId, UUID applicationId);

  @Nullable Application getApplication(@NotNull UUID appId, @NotNull Opts opts);

  @Nullable Application updateApplication(@NotNull UUID appId, @NotNull Application application, @NotNull Opts opts) throws DuplicateApplicationException, OptimisticLockingException;

  @Nullable List<Feature> createApplicationFeature(@NotNull UUID appId, Feature feature, Person person, @NotNull Opts opts) throws DuplicateFeatureException;
  @Nullable List<Feature> updateApplicationFeature(@NotNull UUID appId, String key, Feature feature, @NotNull Opts opts) throws DuplicateFeatureException, OptimisticLockingException;
  @Nullable List<Feature> getApplicationFeatures(@NotNull UUID appId, @NotNull Opts opts);
  @Nullable List<Feature> deleteApplicationFeature(@NotNull UUID appId, String key);
  @Nullable Feature getApplicationFeatureByKey(@NotNull UUID appId, @NotNull String key, @NotNull Opts opts);

  @NotNull Set<UUID> findFeatureEditors(@NotNull UUID appId);
  boolean personIsFeatureEditor(@NotNull UUID appId, @NotNull UUID personId);

  @NotNull ApplicationPermissions findApplicationPermissions(@NotNull UUID appId, @NotNull UUID personId);

  /**
   * Those who can create features
   * @param appId
   * @return
   */
  @NotNull Set<UUID> findFeatureCreators(@NotNull UUID appId);
  boolean personIsFeatureCreator(@NotNull UUID appId, @NotNull UUID personId);

  @NotNull Set<UUID> findFeatureReaders(@NotNull UUID appId);
  boolean personIsFeatureReader(UUID appId, UUID personId);
}
