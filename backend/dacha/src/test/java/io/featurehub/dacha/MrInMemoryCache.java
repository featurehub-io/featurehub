package io.featurehub.dacha;

import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountCacheItem;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 */
public class MrInMemoryCache implements InternalCache {
  private Map<String, EnvironmentCacheItem> environments = new ConcurrentHashMap<>();
  private Map<String, ServiceAccountCacheItem> serviceAccounts = new ConcurrentHashMap<>();

  public MrInMemoryCache() {
    environments.put("1",
      new EnvironmentCacheItem()
      .environment(new Environment().id("1").version(1L))
      .action(PublishAction.CREATE)
      .count(1)
    );
    serviceAccounts.put("2",
      new ServiceAccountCacheItem()
        .serviceAccount(new ServiceAccount().id("2").version(1L).apiKey("apikey"))
        .count(1)
        .action(PublishAction.CREATE)
      );
  }

  @Override
  public boolean cacheComplete() {
    return true;
  }

  @Override
  public void onCompletion(Runnable notify) {
  }

  @Override
  public void clear() {
  }

  @Override
  public Stream<EnvironmentCacheItem> environments() {
    return environments.values().stream();
  }

  @Override
  public Stream<ServiceAccountCacheItem> serviceAccounts() {
    return serviceAccounts.values().stream();
  }

  @Override
  public void serviceAccount(ServiceAccountCacheItem sa) {
    serviceAccounts.put(sa.getServiceAccount().getId(), sa);
  }

  @Override
  public void environment(EnvironmentCacheItem e) {
    environments.put(e.getEnvironment().getId(), e);
  }

  @Override
  public FeatureCollection getFeaturesByEnvironmentAndServiceAccount(String environmentId, String apiKey) {
    return null;
  }


  @Override
  public void updateFeatureValue(FeatureValueCacheItem fv) {
  }
}
