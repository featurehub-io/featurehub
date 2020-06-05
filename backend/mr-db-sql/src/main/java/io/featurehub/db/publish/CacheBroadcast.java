package io.featurehub.db.publish;

import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.ServiceAccountCacheItem;

public interface CacheBroadcast {
  void publishEnvironment(EnvironmentCacheItem eci);

  void publishServiceAccount(ServiceAccountCacheItem saci);

  void publishFeature(FeatureValueCacheItem feature);
}
