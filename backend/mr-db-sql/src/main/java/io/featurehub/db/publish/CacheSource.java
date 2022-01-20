package io.featurehub.db.publish;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.dacha.model.PublishAction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface CacheSource {
  void registerCache(String cacheName, CacheBroadcast cacheBroadcast);
  void publishToCache(String cacheName);

  void deleteFeatureChange(DbApplicationFeature feature, UUID environmentId);

  /**
   * Whenever a service account changes or at start up, publish it
   */
  void updateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction);

  void deleteServiceAccount(UUID id);

  /**
   * whenever an environment changes or at start up, publish it.
   *
   * This is used to cache environments and their feature values.
   */
  void updateEnvironment(DbEnvironment environment, PublishAction publishAction);

  void deleteEnvironment(UUID id);

  void publishFeatureChange(@NotNull DbApplicationFeature appFeature, @NotNull PublishAction update);

  /*
   * This one is used by the Archive to preserve the original feature key
   */
  void publishFeatureChange(@NotNull DbApplicationFeature appFeature, @NotNull PublishAction update,
                            @NotNull String featureKey);
  /**
   * Whenever a feature value changes in any way, publish it out.
   */
  void publishFeatureChange(@NotNull DbFeatureValue strategy);


  void publishRolloutStrategyChange(DbRolloutStrategy rs);
}
