package io.featurehub.dacha;

import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountCacheItem;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 */
public class MrInMemoryCache implements InternalCache {
  private Map<UUID, EnvironmentCacheItem> environments = new ConcurrentHashMap<>();
  private Map<UUID, ServiceAccountCacheItem> serviceAccounts = new ConcurrentHashMap<>();

  public MrInMemoryCache() {
    UUID envId = UUID.randomUUID();
    environments.put(envId,
      new EnvironmentCacheItem()
      .environment(new Environment().id(envId).version(1L))
      .action(PublishAction.CREATE)
      .count(1)
    );
    UUID svcId = UUID.randomUUID();
    serviceAccounts.put(svcId,
      new ServiceAccountCacheItem()
        .serviceAccount(new ServiceAccount().id(svcId).version(1L).apiKeyServerSide("apikey")
          .apiKeyClientSide("apikey2#2"))
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
  public FeatureCollection getFeaturesByEnvironmentAndServiceAccount(UUID environmentId, String apiKey) {
    return null;
  }


  @Override
  public void updateFeatureValue(FeatureValueCacheItem fv) {
  }

  @Override
  public EnvironmentCacheItem findEnvironment(UUID environmentId) {
    return environments.get(environmentId);
  }
}
