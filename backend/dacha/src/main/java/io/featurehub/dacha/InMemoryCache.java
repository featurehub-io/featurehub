package io.featurehub.dacha;

import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import io.featurehub.mr.model.ServiceAccountPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryCache implements InternalCache {
  private static final Logger log = LoggerFactory.getLogger(InMemoryCache.class);
  private boolean wasServiceAccountComplete;
  private boolean wasEnvironmentComplete;
  private final Map<String, EnvironmentCacheItem> environments = new ConcurrentHashMap<>();
  // <environment id, <feature id, fv cache>>
  private final Map<String, Map<String, FeatureValueCacheItem>> environmentFeatures = new ConcurrentHashMap<>();
  private final Map<String, ServiceAccountCacheItem> serviceAccounts = new ConcurrentHashMap<>();
  // <sdk id + / + environment id ==> ServiceAccount. if null, none maps, otherwise you can do something with it
  private final Map<String, ServiceAccountPermission> serviceAccountPlusEnvIdToEnvIdMap = new ConcurrentHashMap<>();
  // Map<apiKey, serviceAccountId>
//  private final Map<String, String> apiKeyToServiceAccountKeyMap = new ConcurrentHashMap<>();
  private final Map<String, List<FeatureValueCacheItem>> valuesForUnpublishedEnvironments = new ConcurrentHashMap<>();
  private Runnable notify;

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
  public Stream<EnvironmentCacheItem> environments() {
    // make sure we update how many we have
    int size = environments.size();

    if (size == 0) {
      return Stream.of(new EnvironmentCacheItem().action(PublishAction.EMPTY));
    }

    return environments.values().stream().peek(sa -> sa.setCount(size));
  }

  @Override
  public Stream<ServiceAccountCacheItem> serviceAccounts() {
    // make sure we update how many we have
    int size = serviceAccounts.size();

    if (size == 0) {
      return Stream.of(new ServiceAccountCacheItem().action(PublishAction.EMPTY));
    }

    return serviceAccounts.values().stream().peek(sa -> sa.setCount(size));
  }

  @Override
  public void serviceAccount(ServiceAccountCacheItem sa) {
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

    ServiceAccountCacheItem existing = serviceAccounts.get(sa.getServiceAccount().getId());

    if (sa.getAction() == PublishAction.CREATE || sa.getAction() == PublishAction.UPDATE) {
      if (existing == null || sa.getServiceAccount().getVersion() >= existing.getServiceAccount().getVersion()) {
        updateServiceAccountEnvironmentCache(sa.getServiceAccount(), serviceAccounts.get(sa.getServiceAccount().getId()));
        serviceAccounts.put(sa.getServiceAccount().getId(), sa);

//        apiKeyToServiceAccountKeyMap.put(sa.getServiceAccount().getApiKeyClientSide(), sa.getServiceAccount().getId());
//        apiKeyToServiceAccountKeyMap.put(sa.getServiceAccount().getApiKeyServerSide(), sa.getServiceAccount().getId());

        log.debug("have sa {} / {} : {} + {} -> {} : {}", serviceAccounts.size(), sa.getCount(),
          sa.getServiceAccount().getApiKeyClientSide(),
          sa.getServiceAccount().getApiKeyServerSide(),
          sa.getServiceAccount().getName(), sa.getServiceAccount().getId());
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
      ServiceAccountCacheItem removeAccount = serviceAccounts.remove(sa.getServiceAccount().getId());
      if (removeAccount != null) { // make sure we remove it from the api key list as well
        updateServiceAccountEnvironmentCache(null, removeAccount);
//        apiKeyToServiceAccountKeyMap.remove(removeAccount.getServiceAccount().getApiKeyServerSide());
//        apiKeyToServiceAccountKeyMap.remove(removeAccount.getServiceAccount().getApiKeyClientSide());
      }
    }


  }

  /**
   * This is crucial to keep up to date as it keeps a map of the sdkKey + env id against the permission in that
   * environment. It is the only thing that we check when getting asked for data.
   */
  private void updateServiceAccountEnvironmentCache(ServiceAccount serviceAccount, ServiceAccountCacheItem oldServiceAccount) {
    if (oldServiceAccount != null) {
      oldServiceAccount.getServiceAccount().getPermissions().forEach(perm -> {
          log.debug("update cache, removing {} / {} :{}",
            serviceAccount.getApiKeyClientSide(),
            serviceAccount.getApiKeyServerSide(),
            perm);
          serviceAccountPlusEnvIdToEnvIdMap.remove(sdkKeyEnvId(serviceAccount.getApiKeyServerSide(),
            perm.getEnvironmentId()));
          serviceAccountPlusEnvIdToEnvIdMap.remove(sdkKeyEnvId(serviceAccount.getApiKeyClientSide(),
            perm.getEnvironmentId()));
        }
      );
    }

    if (serviceAccount != null) {
      serviceAccount.getPermissions().forEach(perm -> {
        log.debug("update cache, adding {}:{}", serviceAccount.getApiKeyClientSide(), perm);
          serviceAccountPlusEnvIdToEnvIdMap.put(sdkKeyEnvId(serviceAccount.getApiKeyServerSide(), perm.getEnvironmentId()), perm);
          serviceAccountPlusEnvIdToEnvIdMap.put(sdkKeyEnvId(serviceAccount.getApiKeyClientSide(), perm.getEnvironmentId()), perm);
        }
      );
    }
  }

  // we can only _delete_ items here, when service accounts are removed from environments, as permissions are not passed down
  private void updateServiceAccountEnvironmentByEnvironment(EnvironmentCacheItem newItem, EnvironmentCacheItem oldCacheItem) {
    if (oldCacheItem != null) {

      final String envId = oldCacheItem.getEnvironment().getId();
      if (newItem != null) {
        Map<String, Boolean> existing = oldCacheItem.getServiceAccounts().stream()
          .collect(Collectors.toMap(s -> sdkKeyEnvId(s.getApiKeyClientSide(), envId), s -> Boolean.TRUE));
        existing.putAll(oldCacheItem.getServiceAccounts().stream()
          .collect(Collectors.toMap(s -> sdkKeyEnvId(s.getApiKeyServerSide(), envId), s -> Boolean.TRUE)));

        // take out from existing all the ones that exist in the new item
        newItem
            .getServiceAccounts()
            .forEach(
                sa -> {
                  existing.remove(sdkKeyEnvId(sa.getApiKeyServerSide(), newItem.getEnvironment().getId()));
                  existing.remove(sdkKeyEnvId(sa.getApiKeyClientSide(), newItem.getEnvironment().getId()));
                });

        // ones that are left we have to delete
        existing.keySet().forEach(k -> {
          log.debug("Environment update, SDK/Env keys removed from acceptable map {}", k);
          serviceAccountPlusEnvIdToEnvIdMap.remove(k);
        });
      } else {
        oldCacheItem.getServiceAccounts().forEach(s -> {
          log.debug("Environment update, SDK/Env keys removed from acceptable map {}:{}", s.getApiKeyClientSide(), envId);
          serviceAccountPlusEnvIdToEnvIdMap.remove(sdkKeyEnvId(s.getApiKeyServerSide(), envId));
          serviceAccountPlusEnvIdToEnvIdMap.remove(sdkKeyEnvId(s.getApiKeyClientSide(), envId));
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
  public void environment(EnvironmentCacheItem e) {
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

    final String envId = e.getEnvironment().getId();
    EnvironmentCacheItem existing = environments.get(envId);

    if (e.getAction() == PublishAction.CREATE || e.getAction() == PublishAction.UPDATE) {
      if (existing == null || e.getEnvironment().getVersion() >= existing.getEnvironment().getVersion()) {
        updateServiceAccountEnvironmentByEnvironment(e, environments.get(envId));
        environments.put(envId, e);

        replaceEnvironmentFeatures(e);

        List<FeatureValueCacheItem> unpublishedEnvironmentItems = valuesForUnpublishedEnvironments.remove(envId);
        if (unpublishedEnvironmentItems != null) {
          unpublishedEnvironmentItems.forEach(this::updateFeatureValue);
        }

        log.debug("have env {} of {} name {} : /default/{}/ {}", environments.size(), e.getCount(), e.getEnvironment().getName(), envId, sAccounts(e));

        if (!wasEnvironmentComplete && e.getCount() == environments.size()) {
          wasEnvironmentComplete = true;

          if (wasServiceAccountComplete && notify != null) {
            logEmptyCacheOnStart();
            notify.run();
          }
        }
      }
    }

    if (e.getAction() == PublishAction.DELETE && existing != null) {
      updateServiceAccountEnvironmentByEnvironment(null, environments.get(envId));
      environments.remove(envId);
    }
  }

  private String sAccounts(EnvironmentCacheItem e) {
    if (e.getServiceAccounts() == null || e.getServiceAccounts().size() == 0) {
      return "none";
    }

    return e.getServiceAccounts().stream().map(ServiceAccount::getId).collect(Collectors.joining());
  }

  /**
   * we just received a whole updated environment with its associated features, so replace in-situ anything we had.
   *
   * @param e - the environment
   */
  private void replaceEnvironmentFeatures(EnvironmentCacheItem e) {
    // get all of the values together
    Map<String, FeatureValue> values = e.getFeatureValues().stream()
      .collect(Collectors.toMap(FeatureValue::getKey, Function.identity()));

    // now create a map of featureId -> feature + feature-value that clients can consume easily.
    environmentFeatures.put(e.getEnvironment().getId(),
      e.getEnvironment().getFeatures().stream()
        .collect(Collectors.toMap(Feature::getId, f -> {
          final FeatureValue value = values.get(f.getKey());
          final FeatureValueCacheItem fvci = new FeatureValueCacheItem().feature(f).value(value).strategies(value.getRolloutStrategies());
          value.setRolloutStrategies(null);
          return fvci;
        })));
  }

  private String sdkKeyEnvId(String apiKey, String environmentId) {
    return apiKey + "/" + environmentId;
  }

  @Override
  public FeatureCollection getFeaturesByEnvironmentAndServiceAccount(String environmentId, String apiKey) {
    log.debug("got request for environment `{}` and apiKey `{}`", environmentId, apiKey);

    ServiceAccountPermission sa = serviceAccountPlusEnvIdToEnvIdMap.get(sdkKeyEnvId(apiKey, environmentId));
    if (sa != null && !sa.getPermissions().isEmpty()) {  // any permission is good enough to read
      return new FeatureCollection(environmentFeatures.get(environmentId).values(), sa);
    }

    log.warn("No apiKey for `{}`", apiKey);
    return null; // no such service account
  }

  @Override
  public void updateFeatureValue(FeatureValueCacheItem fv) {
//    log.debug("received update {}", fv);

    EnvironmentCacheItem eci = environments.get(fv.getEnvironmentId());
    if (eci == null) {
      log.debug("received feature for unknown environment: `{}`: {}", fv.getEnvironmentId(), fv);
      List<FeatureValueCacheItem> fvs = valuesForUnpublishedEnvironments.computeIfAbsent(fv.getEnvironmentId(), key -> new ArrayList<>());
      fvs.add(fv);
    } else {
      Map<String, FeatureValueCacheItem> featureMap = environmentFeatures.get(fv.getEnvironmentId());

      if (featureMap != null) {
        FeatureValueCacheItem feature = featureMap.get(fv.getFeature().getId());

        if (feature == null) {
          if (fv.getAction() == PublishAction.CREATE) {
            log.info("received brand new feature `{}` for environment `{}`", fv.getFeature().getKey(), fv.getEnvironmentId());
            featureMap.put(fv.getFeature().getId(), fv);
          } else {
            log.error("received a feature value update for feature key `{}` in environment `{}` where the feature does not exist.", fv.getFeature().getKey(), fv.getEnvironmentId());
          }

          return;
        }

        if (fv.getAction() == PublishAction.CREATE || fv.getAction() == PublishAction.UPDATE) {
          boolean featureChanged = feature.getFeature().getVersion() < fv.getFeature().getVersion();
          if (featureChanged) {
            feature.feature(fv.getFeature()); // replace the feature it has changed
          }
          // now we know the feature + feature-value
          //log.info("feature new  {} vs old {}", fv, feature);
          if (fv.getValue().getVersion() != null) {
            if (feature.getValue() == null) {
              feature.value(fv.getValue());
              feature.strategies(fv.getStrategies());
            } else if (feature.getValue().getVersion() == null || (fv.getValue().getVersion() != null && feature.getValue().getVersion() < fv.getValue().getVersion())) {
              feature.value(fv.getValue());
              feature.strategies(fv.getStrategies());
              log.trace("replacing with {}", fv.getFeature());
            } else if (!featureChanged) {
              log.warn("attempted to remove/update envId:key {}:{} that is older than existing version, ignoring", fv.getEnvironmentId(), fv.getFeature().getKey());
            }
          } else {
            log.trace("received feature update with no version {}", fv);
          }
        } else if (fv.getAction() == PublishAction.DELETE) {
          log.debug("removing feature value from feature key `{}` in environment `{}`", fv.getFeature().getKey(), fv.getEnvironmentId());
          featureMap.remove(fv.getFeature().getId());
        }
      } else {
        log.warn("received update for non existent environment `{}`", fv.getEnvironmentId());
      }
    }
  }
}
