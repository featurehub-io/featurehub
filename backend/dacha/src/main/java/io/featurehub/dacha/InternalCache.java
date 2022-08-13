package io.featurehub.dacha;

import io.featurehub.dacha.model.CacheEnvironmentFeature;
import io.featurehub.dacha.model.CacheServiceAccountPermission;
import io.featurehub.dacha.model.PublishEnvironment;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.dacha.model.PublishServiceAccount;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

public interface InternalCache extends CacheUpdateListener {
  interface FeatureValues {
    Collection<CacheEnvironmentFeature> getFeatures();
    PublishEnvironment getEnvironment();
    String getEtag();
  }

  class FeatureCollection {
    public final FeatureValues features;
    public final CacheServiceAccountPermission perms;
    public final UUID serviceAccountId;

    public FeatureCollection(FeatureValues features, CacheServiceAccountPermission perms, UUID serviceAccountId) {
      this.features = features;
      this.perms = perms;
      this.serviceAccountId = serviceAccountId;
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

  Stream<PublishEnvironment> environments();
  Stream<PublishServiceAccount> serviceAccounts();

  FeatureCollection getFeaturesByEnvironmentAndServiceAccount(UUID environmentId, String apiKey);


  PublishEnvironment findEnvironment(UUID environmentId);
}
