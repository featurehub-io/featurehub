package io.featurehub.db.publish;

import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbEnvironmentFeatureStrategy;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.PublishAction;

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
  public void publishFeatureChange(DbEnvironmentFeatureStrategy strategy) {

  }

  @Override
  public void deleteFeatureChange(DbApplicationFeature feature, String environmentId) {

  }

  @Override
  public void updateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction) {

  }

  @Override
  public void deleteServiceAccount(UUID id) {

  }


  @Override
  public void updateEnvironment(DbEnvironment environment) {

  }

  @Override
  public void deleteEnvironment(UUID id) {

  }

  @Override
  public void publishFeatureChange(DbApplicationFeature appFeature, PublishAction update) {

  }


}
