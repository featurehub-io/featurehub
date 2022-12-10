package io.featurehub.dacha;

import io.featurehub.dacha.model.CacheEnvironmentFeature;
import io.featurehub.dacha.model.CacheFeature;
import io.featurehub.dacha.model.CacheFeatureValue;
import io.featurehub.dacha.model.CacheServiceAccount;
import io.featurehub.dacha.model.CacheServiceAccountPermission;
import io.featurehub.dacha.model.PublishAction;
import io.featurehub.dacha.model.PublishEnvironment;
import io.featurehub.dacha.model.PublishFeatureValue;
import io.featurehub.dacha.model.PublishServiceAccount;
import io.featurehub.enricher.EnrichmentEnvironment;
import io.featurehub.enricher.FeatureEnrichmentCache;
import io.featurehub.metrics.MetricsCollector;
import io.prometheus.client.Gauge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryCache implements InternalCache, FeatureEnrichmentCache {
  private static final Logger log = LoggerFactory.getLogger(InMemoryCache.class);
  private boolean wasServiceAccountComplete;
  private boolean wasEnvironmentComplete;
  private final Map<UUID, PublishEnvironment> environments = new ConcurrentHashMap<>();
  // <environment id, <feature id, fv cache>>
  private final Map<UUID, EnvironmentFeatures> environmentFeatures = new ConcurrentHashMap<>();
  private final Map<UUID, PublishServiceAccount> serviceAccounts = new ConcurrentHashMap<>();
  // <service account UUID id + / + environment UUID id ==> ServiceAccount. if null, none maps, otherwise you can do
  // something with it
  private final Map<String, CacheServiceAccountPermission> serviceAccountPlusEnvIdToEnvIdMap = new ConcurrentHashMap<>();
  // Map<apiKey (client and server), serviceAccountId UUID>
  private final Map<String, UUID> apiKeyToServiceAccountKeyMap = new ConcurrentHashMap<>();
  private final Map<UUID, List<PublishFeatureValue>> valuesForUnpublishedEnvironments = new ConcurrentHashMap<>();
  private Runnable notify;

  private final Gauge environmentGauge = MetricsCollector.Companion.gauge("dacha_environment_gauge",
    "Number of active environments");
  private final Gauge serviceAccountsGauge = MetricsCollector.Companion.gauge("dacha_service_accounts_gauge",
    "Number of active service accounts");

  @Override
  public boolean cacheComplete() {
    return wasServiceAccountComplete && wasEnvironmentComplete;
  }

  @Override
  public void onCompletion(Runnable notify) {
    this.notify = notify;
  }

  @Override
  public void clear() {
    environments.clear();
    serviceAccounts.clear();
    valuesForUnpublishedEnvironments.clear();
    wasServiceAccountComplete = false;
    wasEnvironmentComplete = false;
  }

  @Override
  public Stream<PublishEnvironment> environments() {
    // make sure we update how many we have
    int size = environments.size();

    if (size == 0) {
      return Stream.of(new PublishEnvironment().action(PublishAction.EMPTY));
    }

    return environments.values().stream().peek(env -> env.setCount(size));
  }

  @Override
  public Stream<PublishServiceAccount> serviceAccounts() {
    // make sure we update how many we have
    int size = serviceAccounts.size();

    if (size == 0) {
      return Stream.of(new PublishServiceAccount().action(PublishAction.EMPTY));
    }

    return serviceAccounts.values().stream().peek(sa -> sa.setCount(size));
  }

  @Override
  public void updateServiceAccount(PublishServiceAccount sa) {
    if (sa.getAction() == PublishAction.EMPTY) {
      if (!wasServiceAccountComplete) {
        wasServiceAccountComplete = true;
        if (wasEnvironmentComplete && notify != null) {
          logEmptyCacheOnStart();
          notify.run();
        }
      }

      return;
    }

    PublishServiceAccount existing = serviceAccounts.get(sa.getServiceAccount().getId());

    if (sa.getAction() == PublishAction.CREATE || sa.getAction() == PublishAction.UPDATE) {
      // the version check needs to be >= because the fields INSIDE the service account don't cause a service account's
      // version to change, e.g. the permissions - ONLY the service account object itself. This will however had the
      // side
      // effect that when a new Dacha comes online, all the existing Dachas will update their caches from the one who
      // responds with its contents.

      if (existing == null
          || (existing.getServiceAccount() != null &&
        sa.getServiceAccount().getVersion() >= existing.getServiceAccount().getVersion())) {
        updateServiceAccountEnvironmentCache(sa.getServiceAccount(), serviceAccounts.get(sa.getServiceAccount().getId()));
        serviceAccounts.put(sa.getServiceAccount().getId(), sa);

        log.trace("have sa {} / {} : {} + {} -> {}", serviceAccounts.size(), sa.getCount(),
          sa.getServiceAccount().getApiKeyClientSide(),
          sa.getServiceAccount().getApiKeyServerSide(),
          sa.getServiceAccount().getId());
        if (!wasServiceAccountComplete && sa.getCount() == serviceAccounts.size()) {
          wasServiceAccountComplete = true;

          if (wasEnvironmentComplete && notify != null) {
            logEmptyCacheOnStart();
            notify.run();
          }
        }
      }
    }

    if (sa.getAction() == PublishAction.DELETE && existing != null) {
      PublishServiceAccount removeAccount = serviceAccounts.remove(sa.getServiceAccount().getId());

      if (removeAccount != null) { // make sure we remove it from the api key list as well
        updateServiceAccountEnvironmentCache(null, removeAccount);
      }
    }

    serviceAccountsGauge.set(serviceAccounts.size());
  }

  String serviceAccountIdPlusEnvId(UUID serviceAccountId, UUID environmentId) {
    return serviceAccountId.toString() + "/" + environmentId.toString();
  }

  /**
   * This is crucial to keep up to date as it keeps a map of the sdkKey + env id against the permission in that
   * environment. It is the only thing that we check when getting asked for data.
   */
  private void updateServiceAccountEnvironmentCache(CacheServiceAccount serviceAccount, PublishServiceAccount oldServiceAccount) {
    if (oldServiceAccount != null && oldServiceAccount.getServiceAccount() != null) {
      oldServiceAccount.getServiceAccount().getPermissions().forEach(perm -> {
          log.trace("update cache, removing {} :{} - {}",
            serviceAccount.getId(),
            serviceAccount.getApiKeyServerSide(),
            perm);

          // remove them from the service account + env id map
          serviceAccountPlusEnvIdToEnvIdMap.remove(serviceAccountIdPlusEnvId(serviceAccount.getId(),
            perm.getEnvironmentId()));
          // remove them from the apikey -> serviceid map
        }
      );

      apiKeyToServiceAccountKeyMap.remove(oldServiceAccount.getServiceAccount().getApiKeyClientSide());
      apiKeyToServiceAccountKeyMap.remove(oldServiceAccount.getServiceAccount().getApiKeyServerSide());
    }

    if (serviceAccount != null) {
      serviceAccount.getPermissions().forEach(perm -> {
        log.trace("update cache, adding {}:{}", serviceAccount.getApiKeyClientSide(), perm);
        serviceAccountPlusEnvIdToEnvIdMap.put(serviceAccountIdPlusEnvId(serviceAccount.getId(),
          perm.getEnvironmentId()), perm);
        }
      );

      apiKeyToServiceAccountKeyMap.put(serviceAccount.getApiKeyClientSide(), serviceAccount.getId());
      apiKeyToServiceAccountKeyMap.put(serviceAccount.getApiKeyServerSide(), serviceAccount.getId());
    }
  }

  // we can only _delete_ items here, when service accounts are removed from environments, as permissions are not passed down
  private void removeServiceAccountsFromEnvironment(PublishEnvironment newItem, PublishEnvironment oldCacheItem) {
    if (oldCacheItem != null) {

      final UUID envId = oldCacheItem.getEnvironment().getId();
      if (newItem != null) {
        Map<String, Boolean> existing = oldCacheItem.getServiceAccounts().stream()
          .collect(Collectors.toMap(s -> serviceAccountIdPlusEnvId(s, envId), s -> Boolean.TRUE));

        // take out from existing all the ones that exist in the new item
        newItem
            .getServiceAccounts()
            .forEach(
                sa -> existing.remove(serviceAccountIdPlusEnvId(sa, newItem.getEnvironment().getId())));

        // ones that are left we have to delete
        existing.keySet().forEach(k -> {
          log.debug("Environment update, API keys removed from acceptable map {}", k);
          serviceAccountPlusEnvIdToEnvIdMap.remove(k);
        });
      } else {
        oldCacheItem.getServiceAccounts().forEach(s -> {
          log.debug("Environment update, API keys removed from acceptable map {}:{}", s, envId);
          serviceAccountPlusEnvIdToEnvIdMap.remove(serviceAccountIdPlusEnvId(s, envId));
        });
      }
    }
  }

  private void logEmptyCacheOnStart() {
    if (serviceAccounts.size() == 0 && environments.size() == 0) {
      log.info("Started Dacha with an empty cache as we appear to be a new server.");
    }
  }

  @Override
  public void updateEnvironment(PublishEnvironment e) {
    if (e.getAction() == PublishAction.EMPTY) {
      if (!wasEnvironmentComplete) {
        wasEnvironmentComplete = true;
        if (wasServiceAccountComplete && notify != null) {
          logEmptyCacheOnStart();
          notify.run();
        }
      }

      return;
    }

    final UUID envId = e.getEnvironment().getId();
    PublishEnvironment existing = environments.get(envId);

    if ((e.getAction() == PublishAction.CREATE || e.getAction() == PublishAction.UPDATE)) {
      // the version check needs to be >= because the fields INSIDE the environment don't cause an environment's
      // version to change, e.g. the features - ONLY the environment object itself. This will however had the side
      // effect that when a new Dacha comes online, all the existing Dachas will update their caches from the one who
      // responds with its contents.
      if (existing == null || e.getEnvironment().getVersion() >= existing.getEnvironment().getVersion()) {
        removeServiceAccountsFromEnvironment(e, environments.get(envId));
        environments.put(envId, e);

        replaceEnvironmentFeatures(e);

        List<PublishFeatureValue> unpublishedEnvironmentItems = valuesForUnpublishedEnvironments.remove(envId);
        if (unpublishedEnvironmentItems != null) {
          unpublishedEnvironmentItems.forEach(this::updateFeatureValue);
        }

        log.debug("have env {} of {} : /default/{}/ {}", environments.size(), e.getCount(), envId, sAccounts(e));

        if (!wasEnvironmentComplete && e.getCount() != null && e.getCount() == environments.size()) {
          wasEnvironmentComplete = true;

          if (wasServiceAccountComplete && notify != null) {
            logEmptyCacheOnStart();
            notify.run();
          }
        }
      }
    }

    if (e.getAction() == PublishAction.DELETE && existing != null) {
      removeServiceAccountsFromEnvironment(null, environments.get(envId));
      environments.remove(envId);
    }

    environmentGauge.set(environments.size());
  }

  private String sAccounts(PublishEnvironment e) {
    if (e.getServiceAccounts().isEmpty()) {
      return "none";
    }

    return e.getServiceAccounts().stream().map(Object::toString).collect(Collectors.joining());
  }

  /**
   * we just received a whole updated environment with its associated features, so replace in-situ anything we had.
   *
   * @param e - the environment
   */
  private void replaceEnvironmentFeatures(PublishEnvironment e) {
    environmentFeatures.put(e.getEnvironment().getId(), new EnvironmentFeatures(e));
  }

  @Override
  public FeatureCollection getFeaturesByEnvironmentAndServiceAccount(UUID environmentId, String apiKey) {
    log.debug("got request for environment `{}` and apiKey `{}`", environmentId, apiKey);

    UUID serviceAccountId = apiKeyToServiceAccountKeyMap.get(apiKey);

    if (serviceAccountId == null) {
      log.warn("No apiKey for `{}`", apiKey);
      return null;
    }

    CacheServiceAccountPermission perm = serviceAccountPlusEnvIdToEnvIdMap.get(serviceAccountIdPlusEnvId(serviceAccountId, environmentId));

    if (perm != null && !perm.getPermissions().isEmpty()) {  // any permission is good enough to read
      final EnvironmentFeatures features = environmentFeatures.get(environmentId);

      if (features != null) {
        return new FeatureCollection(
          features,
            perm, serviceAccountId);
      }
    }

    log.warn("No data for valid apiKey  `{}`", apiKey);
    return null; // no such service account
  }

  @Override
  public void updateFeatureValue(PublishFeatureValue fv) {
    log.debug("received update {}", fv);

    PublishEnvironment eci = environments.get(fv.getEnvironmentId());

    if (eci == null) {
      log.debug("received feature for unknown environment: `{}`: {}, holding", fv.getEnvironmentId(), fv);
      List<PublishFeatureValue> fvs = valuesForUnpublishedEnvironments.computeIfAbsent(fv.getEnvironmentId(), key -> new ArrayList<>());
      fvs.add(fv);

      return;
    }

    updateEdgeCache(eci, fv);
  }

  private void updateEdgeCache(PublishEnvironment eci, PublishFeatureValue fv) {
    EnvironmentFeatures envFeatureBundle = environmentFeatures.get(fv.getEnvironmentId());

    // this environment we have received a feature for doesn't exist
    if (envFeatureBundle == null) {
      log.error("We have an environment but no features for that environment, which is not possible.");
      return;
    }

    @NotNull CacheFeature newFeature = fv.getFeature().getFeature();
    @Nullable CacheFeatureValue newValue = fv.getFeature().getValue();

    CacheEnvironmentFeature existingCachedFeature = envFeatureBundle.get(newFeature.getId());

    // yuk
    int indexOfExistingPublishedCacheEnvironmentFeature = -1;
    int pos = 0;
    for(CacheEnvironmentFeature cef : eci.getFeatureValues()) {
      if (cef.getFeature().getId().equals(newFeature.getId())) {
        indexOfExistingPublishedCacheEnvironmentFeature = pos;
        break;
      }

      pos ++;
    }

    if (indexOfExistingPublishedCacheEnvironmentFeature == -1) {
      log.debug("received new feature {}, adding to the environment", fv );
    }

    if (existingCachedFeature == null) {
      receivedNewFeatureForExistingEnvironmentFeatureCache(fv, envFeatureBundle);
      eci.getFeatureValues().add(fv.getFeature());
      return;
    }

    // we know at this point, we have an existing feature map of this environment, we _don't_ know if this
    // feature exists in this environment however

    @NotNull CacheFeature existingFeature = existingCachedFeature.getFeature();
    @Nullable CacheFeatureValue existingValue = existingCachedFeature.getValue();

    if (fv.getAction() == PublishAction.CREATE || fv.getAction() == PublishAction.UPDATE) {
      // if the feature itself changed, thats enough, change the contents
      if (existingFeature.getVersion() < newFeature.getVersion()) {
        envFeatureBundle.set(fv.getFeature());
        eci.getFeatureValues().remove(indexOfExistingPublishedCacheEnvironmentFeature);
        eci.getFeatureValues().add(fv.getFeature());
        return;
      }

      if (newValue != null) {
        if (existingValue == null || existingValue.getVersion() < newValue.getVersion()) {
          envFeatureBundle.set(fv.getFeature());
          eci.getFeatureValues().remove(indexOfExistingPublishedCacheEnvironmentFeature);
          eci.getFeatureValues().add(fv.getFeature());
          return;
        } else if (existingValue.getValue() == newValue.getVersion()) {
          return; // ignore
        }
      }

      log.warn("attempted to remove/update envId:key {}:{} that is older than existing version, ignoring",
        fv.getEnvironmentId(), newFeature.getKey());
    } else if (fv.getAction() == PublishAction.DELETE) {
      log.debug("removing feature value from feature key `{}` in environment `{}`", newFeature.getKey(),
        fv.getEnvironmentId());

      envFeatureBundle.remove(newFeature.getId());
      eci.getFeatureValues().remove(indexOfExistingPublishedCacheEnvironmentFeature);
    }
  }

  private void receivedNewFeatureForExistingEnvironmentFeatureCache(PublishFeatureValue fv, EnvironmentFeatures featureMap) {
    if (fv.getAction() == PublishAction.CREATE || fv.getAction() == PublishAction.UPDATE) {
      if (fv.getAction() == PublishAction.UPDATE) {
        log.warn("We have received a feature `{}` as an UPDATE that we never received a CREATE for in environment " +
          "`{}`", fv, featureMap);
      }
      log.trace("received brand new feature `{}` for a new environment `{}`",
        fv.getFeature().getFeature().getKey(),
        fv.getEnvironmentId());
      featureMap.set(fv.getFeature());
    } else {
      log.error("received a feature value update for feature key `{}` in environment `{}` where the feature does " +
        "not exist.", fv.getFeature().getFeature().getKey(), fv.getEnvironmentId());
    }
  }

  @Override
  public PublishEnvironment findEnvironment(UUID environmentId) {
    return environments.get(environmentId);
  }

  @NotNull
  @Override
  public EnrichmentEnvironment getEnrichableEnvironment(@NotNull UUID eId) {
    final EnvironmentFeatures efeat = environmentFeatures.get(eId);

    if (efeat == null) {
      throw new RuntimeException("no such environment");
    }

    return new EnrichmentEnvironment(efeat.getFeatures(), efeat.getEnvironment());
  }

  @Override
  public void updateFeature(@NotNull PublishFeatureValue feature) {
    updateFeatureValue(feature);
  }
}
