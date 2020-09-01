package io.featurehub.db.publish;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.mr.model.PublishAction;

import java.util.UUID;

public interface CacheSource {
  void registerCache(String cacheName, CacheBroadcast cacheBroadcast);
  void publishToCache(String cacheName);

  /**
   * Whenever a feature value changes in any way, publish it out.
   *
   */
  void publishFeatureChange(DbFeatureValue strategy);

  void deleteFeatureChange(DbApplicationFeature feature, String environmentId);

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

  void publishFeatureChange(DbApplicationFeature appFeature, PublishAction update);

  void publishRolloutStrategyChange(DbRolloutStrategy rs);
}
