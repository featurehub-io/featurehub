package io.featurehub.dacha;

import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.ServiceAccountCacheItem;

import java.util.Collection;
import java.util.stream.Stream;

public interface InternalCache {
  boolean cacheComplete();
  void onCompletion(Runnable notify);

  void clear();

  Stream<EnvironmentCacheItem> environments();
  Stream<ServiceAccountCacheItem> serviceAccounts();

  void serviceAccount(ServiceAccountCacheItem sa);

  void environment(EnvironmentCacheItem e);

  Collection<FeatureValueCacheItem> getFeaturesByEnvironmentAndServiceAccount(String environmentId, String apiKey);


  void updateFeatureValue(FeatureValueCacheItem fv);
}
