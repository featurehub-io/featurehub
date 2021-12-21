package io.featurehub.dacha;

import io.featurehub.dacha.model.CacheEnvironment;
import io.featurehub.dacha.model.CacheServiceAccount;
import io.featurehub.dacha.model.PublishEnvironment;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.dacha.model.PublishServiceAccount;
import io.featurehub.dacha.model.PublishAction;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 */
public class MrInMemoryCache implements InternalCache {
  private Map<UUID, PublishEnvironment> environments = new ConcurrentHashMap<>();
  private Map<UUID, PublishServiceAccount> serviceAccounts = new ConcurrentHashMap<>();

  public MrInMemoryCache() {
    UUID envId = UUID.randomUUID();
    environments.put(envId,
      new PublishEnvironment()
      .environment(new CacheEnvironment().id(envId).version(1L).features(new ArrayList<>()))
        .serviceAccounts(new ArrayList<>())
        .applicationId(envId)
        .organizationId(envId)
        .portfolioId(envId)
      .action(PublishAction.CREATE)
      .count(1)
    );
    UUID svcId = UUID.randomUUID();
    serviceAccounts.put(svcId,
      new PublishServiceAccount()
        .serviceAccount(new CacheServiceAccount().id(svcId).version(1L).apiKeyServerSide("apikey")
          .apiKeyClientSide("apikey2#2").permissions(new ArrayList<>()))
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
  public Stream<PublishEnvironment> environments() {
    return environments.values().stream();
  }

  @Override
  public Stream<PublishServiceAccount> serviceAccounts() {
    return serviceAccounts.values().stream();
  }

  @Override
  public void updateServiceAccount(PublishServiceAccount sa) {
    serviceAccounts.put(sa.getServiceAccount().getId(), sa);
  }

  @Override
  public void updateEnvironment(PublishEnvironment e) {
    environments.put(e.getEnvironment().getId(), e);
  }

  @Override
  public FeatureCollection getFeaturesByEnvironmentAndServiceAccount(UUID environmentId, String apiKey) {
    return null;
  }


  @Override
  public void updateFeatureValue(PublishFeatureValue fv) {
  }

  @Override
  public PublishEnvironment findEnvironment(UUID environmentId) {
    return environments.get(environmentId);
  }
}
