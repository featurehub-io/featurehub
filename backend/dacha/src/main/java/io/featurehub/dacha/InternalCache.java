package io.featurehub.dacha;

import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import io.featurehub.mr.model.ServiceAccountPermission;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

public interface InternalCache {
  class FeatureCollection {
    public Collection<FeatureValueCacheItem> features;
    public ServiceAccountPermission perms;

    public FeatureCollection(Collection<FeatureValueCacheItem> features, ServiceAccountPermission perms) {
      this.features = features;
      this.perms = perms;
    }
  }

  boolean cacheComplete();
  void onCompletion(Runnable notify);

  void clear();

  Stream<EnvironmentCacheItem> environments();
  Stream<ServiceAccountCacheItem> serviceAccounts();

  void serviceAccount(ServiceAccountCacheItem sa);

  void environment(EnvironmentCacheItem e);

  FeatureCollection getFeaturesByEnvironmentAndServiceAccount(UUID environmentId, String apiKey);

  void updateFeatureValue(FeatureValueCacheItem fv);

  EnvironmentCacheItem findEnvironment(UUID environmentId);
}
