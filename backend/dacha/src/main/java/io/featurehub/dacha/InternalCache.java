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
    public UUID organizationId;
    public UUID portfolioId;
    public UUID applicationId;

    public FeatureCollection(Collection<FeatureValueCacheItem> features, ServiceAccountPermission perms,
                             UUID organizationId, UUID portfolioId, UUID applicationId) {
      this.features = features;
      this.perms = perms;
      this.organizationId = organizationId;
      this.portfolioId = portfolioId;
      this.applicationId = applicationId;
    }
  }

  /**
   * Is this cache complete and ready for requests?
   */
  boolean cacheComplete();

  /*
   * Register an action to complete when the cache is complete
   */
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
