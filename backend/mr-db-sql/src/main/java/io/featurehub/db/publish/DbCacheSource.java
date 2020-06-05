package io.featurehub.db.publish;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbEnvironmentFeatureStrategy;
import io.featurehub.db.model.DbNamedCache;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbEnvironmentFeatureStrategy;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.db.model.query.QDbServiceAccount;
import io.featurehub.db.services.ConvertUtils;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;


public class DbCacheSource implements CacheSource {
  private static final Logger log = LoggerFactory.getLogger(DbCacheSource.class);

  private final ConvertUtils convertUtils;
  private ExecutorService executor;
  @ConfigKey("cache.pool-size")
  private Integer cachePoolSize = 10;
  private Map<String, CacheBroadcast> cacheBroadcasters = new HashMap<>();

  @Inject
  public DbCacheSource(ConvertUtils convertUtils) {
    this.convertUtils = convertUtils;
    DeclaredConfigResolver.resolve(this);
    executor = Executors.newFixedThreadPool(cachePoolSize);
  }

  public void publishToCache(String cacheName) {
    CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

    if (cacheBroadcast != null) {
      final Future<?> saFuture = executor.submit(() -> publishToCacheServiceAccounts(cacheName, cacheBroadcast));
      final Future<?> envFuture = executor.submit(() -> publishCacheEnvironments(cacheName, cacheBroadcast));

      try {
        saFuture.get();
        envFuture.get();
      } catch (Exception e) {
        log.error("Failed to publish cache.", e);
      }
    }
  }

  private void publishToCacheServiceAccounts(final String cacheName, CacheBroadcast cacheBroadcast) {


    QDbServiceAccount saFinder;

    saFinder = serviceAccountsByCacheName(cacheName);

    int count = saFinder.findCount();
    log.info("publishing {} service accounts to cache {}", count, cacheName);

    saFinder.findEach(sa -> {
      ServiceAccountCacheItem saci = new ServiceAccountCacheItem()
        .action(PublishAction.CREATE)
        .serviceAccount(fillServiceAccount(sa))
        .count(count);

      cacheBroadcast.publishServiceAccount(saci);
    });
  }

  private ServiceAccount fillServiceAccount(DbServiceAccount sa) {
    return convertUtils.toServiceAccount(sa, Opts.opts(FillOpts.Permissions));
  }

  private QDbServiceAccount serviceAccountsByCacheName(String cacheName) {
    return new QDbServiceAccount().whenArchived.isNull().portfolio.organization.namedCache.cacheName.eq(cacheName);
  }

  static final Opts environmentOpts = Opts.opts(FillOpts.Features);

  private void publishCacheEnvironments(String cacheName, CacheBroadcast cacheBroadcast) {
    QDbEnvironment envFinder;

    envFinder = environmentsByCacheName(cacheName);

    int count = envFinder.findCount();
    log.info("publishing {} environments to cache {}", count, cacheName);

    envFinder.findEach(env -> {
      EnvironmentCacheItem eci = fillEnvironmentCacheItem(count, env, PublishAction.CREATE);

      cacheBroadcast.publishEnvironment(eci);
    });
  }

  private EnvironmentCacheItem fillEnvironmentCacheItem(int count, DbEnvironment env, PublishAction publishAction) {
    final Set<DbApplicationFeature> features =
       new QDbApplicationFeature().parentApplication.eq(env.getParentApplication()).whenArchived.isNull().findSet();
    Map<UUID, DbEnvironmentFeatureStrategy> envFeatures =
      env.getEnvironmentFeatures()
        .stream()
        .filter(f -> f.getFeature().getWhenArchived() == null)
        .collect(Collectors.toMap(f -> f.getFeature().getId(), Function.identity()));
    final EnvironmentCacheItem eci = new EnvironmentCacheItem()
      .action(publishAction)
      .environment(convertUtils.toEnvironment(env, environmentOpts, features))
      .featureValues(features.stream().map(f -> convertUtils.toFeatureValue(f, envFeatures.get(f.getId()))).collect(Collectors.toList()))
      .serviceAccounts(env.getServiceAccountEnvironments().stream().map(s -> new ServiceAccount().id(s.getServiceAccount().getId().toString())).collect(Collectors.toList()))
      .count(count);

    if (eci.getEnvironment().getId().equals("0cff6cf3-e03b-41ba-a3fa-683df70bd6d5")) {
      log.info("env {}", eci.toString());
    }

    return eci;
  }

  private QDbEnvironment environmentsByCacheName(String cacheName) {
    return new QDbEnvironment().whenArchived.isNull().environmentFeatures.feature.fetch().parentApplication.portfolio.organization.namedCache.cacheName.eq(cacheName);
  }

  @Override
  public void registerCache(String cacheName, CacheBroadcast cacheBroadcast) {
    cacheBroadcasters.put(
      cacheName, cacheBroadcast
    );
  }


  @Override
  public void publishFeatureChange(DbEnvironmentFeatureStrategy strategy) {
    executor.submit(() -> {
      String cacheName = getFeatureValueCacheName(strategy);
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        cacheBroadcast.publishFeature(
          new FeatureValueCacheItem()
            .feature(convertUtils.toFeature(strategy))
            .value(convertUtils.toFeatureValue(strategy))
            .environmentId(strategy.getEnvironment().getId().toString())
            .action(PublishAction.UPDATE));
      }
    });
  }

  @Override
  public void deleteFeatureChange(DbApplicationFeature feature, String environmentId) {
    executor.submit(() -> {
      String cacheName = new QDbNamedCache().organizations.portfolios.applications.eq(feature.getParentApplication()).findOne().getCacheName();
        CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

        if (cacheBroadcast != null) {
          cacheBroadcast.publishFeature(
            new FeatureValueCacheItem()
              .feature(convertUtils.toApplicationFeature(feature, Opts.empty()))
              .environmentId(environmentId)
              .action(PublishAction.DELETE));
        }
    });
  }

  // todo: consider caching
  private String getFeatureValueCacheName(DbEnvironmentFeatureStrategy strategy) {
    return new QDbNamedCache().organizations.portfolios.applications.environments.environmentFeatures.eq(strategy).findOne().getCacheName();
  }

  // this call comes in from the service layer
  @Override
  public void updateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction) {
    executor.submit(() ->  internalUpdateServiceAccount(serviceAccount, publishAction));
  }

  private void internalUpdateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction) {
    String cacheName = new QDbNamedCache().organizations.portfolios.serviceAccounts.eq(serviceAccount).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

    if (cacheName != null) {
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        if (publishAction != PublishAction.DELETE) {
          log.debug("Updating service account {} -> {}", serviceAccount.getId(), serviceAccount.getApiKey());
          cacheBroadcast.publishServiceAccount(new ServiceAccountCacheItem()
            .count(serviceAccountsByCacheName(cacheName).findCount())
            .serviceAccount(fillServiceAccount(serviceAccount))
            .action(publishAction)
          );
        }
      }
    }
  }

  @Override
  public void deleteServiceAccount(UUID id) {
    executor.submit(() -> internalDeleteServiceAccount(id));
  }

  private void internalDeleteServiceAccount(UUID id) {
    String cacheName = new QDbNamedCache().organizations.portfolios.serviceAccounts.id.eq(id).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

    if (cacheName != null) {
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        log.debug("Sending delete for service account `{}`", id);
        cacheBroadcast.publishServiceAccount(new ServiceAccountCacheItem()
          .count(serviceAccountsByCacheName(cacheName).findCount() - 1)  // now one less
          .serviceAccount(new ServiceAccount().id(id.toString())) // just send the id, thats all the cache needs
          .action(PublishAction.DELETE)
        );
      }
    }
  }

  @Override
  public void updateEnvironment(DbEnvironment environment) {
    executor.submit(() ->  internalUpdateEnvironment(environment));
  }

  private void internalUpdateEnvironment(DbEnvironment environment) {
    if (environment != null) {
      String cacheName = new QDbNamedCache().organizations.portfolios.applications.environments.eq(environment).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

      if (cacheName != null) {
        CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

        if (cacheBroadcast != null) {
          final EnvironmentCacheItem environmentCacheItem = fillEnvironmentCacheItem(environmentsByCacheName(cacheName).findCount(), environment, PublishAction.UPDATE);
          cacheBroadcast.publishEnvironment(environmentCacheItem);
        }
      }
    }
  }

  @Override
  public void deleteEnvironment(UUID id) {
    if (id != null) {
      String cacheName = new QDbNamedCache().organizations.portfolios.applications.environments.id.eq(id).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);
      if (cacheName != null) {
        CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

        if (cacheBroadcast != null) {
          log.debug("deleting environment: `{}`", id);
          cacheBroadcast.publishEnvironment(new EnvironmentCacheItem()
            .count(environmentsByCacheName(cacheName).findCount() - 1)
            .environment(new Environment().id(id.toString()))
            .action(PublishAction.DELETE));
        }
      }
    }
  }

  /**
   * unlike pushing out feature values one by one as they change, this can represent the deletion of a feature value
   * across the board.
   * @param appFeature
   * @param action
   */
  private void publishAppLevelFeatureChange(DbApplicationFeature appFeature, PublishAction action) {
    String cacheName = new QDbNamedCache().organizations.portfolios.applications.eq(appFeature.getParentApplication()).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

    if (cacheName != null) {
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        Feature feature = convertUtils.toApplicationFeature(appFeature, Opts.empty());

        Map<UUID, DbEnvironmentFeatureStrategy> featureValues = new HashMap<>();

        if (action != PublishAction.DELETE) {
          // dont' care about values if deleting
          new QDbEnvironmentFeatureStrategy()
            .environment.whenArchived.isNull()
            .environment.parentApplication.eq(appFeature.getParentApplication())
            .feature.eq(appFeature).findEach(fe -> {
            featureValues.put(fe.getEnvironment().getId(), fe);
          });
        }

        new QDbEnvironment().parentApplication.eq(appFeature.getParentApplication()).whenArchived.isNull().findList().forEach(env -> {
          final FeatureValue featureValue = convertUtils.toFeatureValue(appFeature, featureValues.get(env.getId()));
          cacheBroadcast.publishFeature(
            new FeatureValueCacheItem().feature(feature)
              .value(featureValue).environmentId(env.getId().toString()).action(action));
        });
      }
    }
  }

  @Override
  public void publishFeatureChange(DbApplicationFeature appFeature, PublishAction action) {
    executor.submit(() -> publishAppLevelFeatureChange(appFeature, action)); // background as not going away
  }
}
