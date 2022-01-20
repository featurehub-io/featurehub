package io.featurehub.db.publish;

import io.featurehub.dacha.model.PublishAction;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbServiceAccount;

import java.util.UUID;

/**
 *
 */
public class DummyPublisher implements PublishManager, CacheSource {
  @Override
  public void registerCache(String cacheName, CacheBroadcast cacheBroadcast) {

  }

  @Override
  public void publishToCache(String cacheName) {

  }

  @Override
  public void publishFeatureChange(DbFeatureValue strategy) {

  }

  @Override
  public void deleteFeatureChange(DbApplicationFeature feature, UUID environmentId) {

  }

  @Override
  public void updateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction) {

  }

  @Override
  public void deleteServiceAccount(UUID id) {

  }


  @Override
  public void updateEnvironment(DbEnvironment environment, PublishAction publishAction) {

  }

  @Override
  public void deleteEnvironment(UUID id) {

  }

  @Override
  public void publishFeatureChange(DbApplicationFeature appFeature, PublishAction update) {

  }

  @Override
  public void publishFeatureChange(DbApplicationFeature appFeature, PublishAction update, String featureKey) {

  }

  @Override
  public void publishRolloutStrategyChange(DbRolloutStrategy rs) {

  }


}
