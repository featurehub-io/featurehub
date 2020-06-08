package io.featurehub.dacha;

import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
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
  private Map<String, EnvironmentCacheItem> environments = new ConcurrentHashMap<>();
  // <environment id, <feature id, fv cache>>
  private Map<String, Map<String, FeatureValueCacheItem>> environmentFeatures = new ConcurrentHashMap<>();
  private Map<String, ServiceAccountCacheItem> serviceAccounts = new ConcurrentHashMap<>();
  // Map<apiKey, serviceAccountId>
  private Map<String, String> apiKeyToServiceAccountKeyMap = new ConcurrentHashMap<>();
  private Map<String, List<FeatureValueCacheItem>> valuesForUnpublishedEnvironments = new ConcurrentHashMap<>();
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
        serviceAccounts.put(sa.getServiceAccount().getId(), sa);

        apiKeyToServiceAccountKeyMap.put(sa.getServiceAccount().getApiKey(), sa.getServiceAccount().getId());

        log.debug("have sa {} / {} : {} -> {} : {}", serviceAccounts.size(), sa.getCount(), sa.getServiceAccount().getApiKey(), sa.getServiceAccount().getName(), sa.getServiceAccount().getId());
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
        apiKeyToServiceAccountKeyMap.remove(removeAccount.getServiceAccount().getApiKey());
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

    EnvironmentCacheItem existing = environments.get(e.getEnvironment().getId());

    if (e.getAction() == PublishAction.CREATE || e.getAction() == PublishAction.UPDATE) {
      if (existing == null || e.getEnvironment().getVersion() >= existing.getEnvironment().getVersion()) {
        environments.put(e.getEnvironment().getId(), e);

        replaceEnvironmentFeatures(e);

        List<FeatureValueCacheItem> unpublishedEnvironmentItems = valuesForUnpublishedEnvironments.remove(e.getEnvironment().getId());
        if (unpublishedEnvironmentItems != null) {
          unpublishedEnvironmentItems.forEach(this::updateFeatureValue);
        }

        log.debug("have env {} of {} name {} : /default/{}/ {}", environments.size(), e.getCount(), e.getEnvironment().getName(), e.getEnvironment().getId(), sAccounts(e) );

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
      environments.remove(e.getEnvironment().getId());
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
   * @param e - the environment
   */
  private void replaceEnvironmentFeatures(EnvironmentCacheItem e) {
    // get all of the values together
    Map<String, FeatureValue> values = e.getFeatureValues().stream()
      .collect(Collectors.toMap(FeatureValue::getKey, Function.identity()));

    // now create a map of featureId -> feature + feature-value that clients can consume easily.
    environmentFeatures.put(e.getEnvironment().getId(),
      e.getEnvironment().getFeatures().stream()
        .collect(Collectors.toMap(Feature::getId, f -> new FeatureValueCacheItem().feature(f).value(values.get(f.getKey())))));
  }

  @Override
  public Collection<FeatureValueCacheItem> getFeaturesByEnvironmentAndServiceAccount(String environmentId, String apiKey) {
    log.debug("got request for environment `{}` and apiKey `{}`", environmentId, apiKey);
    String serviceAccountId = apiKeyToServiceAccountKeyMap.get(apiKey);

    if (serviceAccountId == null) {
      log.warn("No apiKey for `{}`", apiKey);
      return null; // no such service account
    }

    EnvironmentCacheItem env = environments.get(environmentId);

    if (env == null) {
      log.warn("No environment for `{}`", environmentId);
      return null; // no such environment
    }

    // todo: optimization
    if (env.getServiceAccounts().stream().anyMatch(sa -> serviceAccountId.equals(sa.getId()))) {
      final Map<String, FeatureValueCacheItem> fciMap = environmentFeatures.get(environmentId);
//      log.info("matched environment {}: {} vs {}", env.getEnvironment().getName(), env.getFeatureValues().size(), fciMap.size());
      return fciMap.values();
    }

    log.warn("No service accounts in that environment matched `{}`", apiKey);

    return null;
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
          if (fv.getValue().getVersion() != null) {
            if (feature.getValue() == null) {
              feature.value(fv.getValue());
            } else if ( feature.getValue().getVersion() == null || (fv.getValue().getVersion() != null && feature.getValue().getVersion() < fv.getValue().getVersion())) {
              feature.value(fv.getValue());
//              log.trace("replacing with {}", fv.getFeature());
            } else if (!featureChanged) {
              log.warn("attempted to remove/update envId:key {}:{} that is older than existing version, ignoring", fv.getEnvironmentId(), fv.getFeature().getKey());
            }
          } else {
//            log.debug("received feature update with no version {}", fv);
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
