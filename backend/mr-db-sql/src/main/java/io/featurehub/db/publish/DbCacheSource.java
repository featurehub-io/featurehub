package io.featurehub.db.publish;

import cd.connect.app.config.ConfigKey;
import cd.connect.app.config.DeclaredConfigResolver;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.Opts;
import io.featurehub.db.model.DbApplicationFeature;
import io.featurehub.db.model.DbEnvironment;
import io.featurehub.db.model.DbFeatureValue;
import io.featurehub.db.model.DbNamedCache;
import io.featurehub.db.model.DbRolloutStrategy;
import io.featurehub.db.model.DbServiceAccount;
import io.featurehub.db.model.DbStrategyForFeatureValue;
import io.featurehub.db.model.query.QDbApplication;
import io.featurehub.db.model.query.QDbApplicationFeature;
import io.featurehub.db.model.query.QDbEnvironment;
import io.featurehub.db.model.query.QDbFeatureValue;
import io.featurehub.db.model.query.QDbNamedCache;
import io.featurehub.db.model.query.QDbOrganization;
import io.featurehub.db.model.query.QDbPortfolio;
import io.featurehub.db.model.query.QDbServiceAccount;
import io.featurehub.db.model.query.QDbStrategyForFeatureValue;
import io.featurehub.db.services.Conversions;
import io.featurehub.mr.model.Environment;
import io.featurehub.mr.model.EnvironmentCacheItem;
import io.featurehub.mr.model.Feature;
import io.featurehub.mr.model.FeatureValue;
import io.featurehub.mr.model.FeatureValueCacheItem;
import io.featurehub.mr.model.FeatureValueType;
import io.featurehub.mr.model.PublishAction;
import io.featurehub.mr.model.RolloutStrategy;
import io.featurehub.mr.model.ServiceAccount;
import io.featurehub.mr.model.ServiceAccountCacheItem;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

  private final Conversions convertUtils;
  private ExecutorService executor;
  @ConfigKey("cache.pool-size")
  private Integer cachePoolSize = 10;
  private Map<String, CacheBroadcast> cacheBroadcasters = new HashMap<>();

  @Inject
  public DbCacheSource(Conversions convertUtils) {
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

    if (count == 0) {
      log.info("database has no service accounts, publishing empty cache indicator.");
      cacheBroadcast.publishServiceAccount(new ServiceAccountCacheItem().action(PublishAction.EMPTY));
    } else {
      log.info("publishing {} service accounts to cache {}", count, cacheName);

      saFinder.findEach(sa -> {
        ServiceAccountCacheItem saci = new ServiceAccountCacheItem()
          .action(PublishAction.CREATE)
          .serviceAccount(fillServiceAccount(sa))
          .count(count);

        cacheBroadcast.publishServiceAccount(saci);
      });
    }
  }

  private ServiceAccount fillServiceAccount(DbServiceAccount sa) {
    return convertUtils.toServiceAccount(sa, Opts.opts(FillOpts.Permissions, FillOpts.IgnoreEmptyPermissions));
  }

  private QDbServiceAccount serviceAccountsByCacheName(String cacheName) {
    return new QDbServiceAccount().whenArchived.isNull().portfolio.organization.namedCache.cacheName.eq(cacheName);
  }

  private void publishCacheEnvironments(String cacheName, CacheBroadcast cacheBroadcast) {
    QDbEnvironment envFinder;

    envFinder = environmentsByCacheName(cacheName);

    int count = envFinder.findCount();

    if (count == 0) {
      log.info("database has no environments, publishing empty environments indicator.");
      cacheBroadcast.publishEnvironment(new EnvironmentCacheItem().action(PublishAction.EMPTY));
    } else {
      log.info("publishing {} environments to cache {}", count, cacheName);

      envFinder.findEach(env -> {
        executor.submit(() -> {
          EnvironmentCacheItem eci = fillEnvironmentCacheItem(count, env, PublishAction.CREATE);

          cacheBroadcast.publishEnvironment(eci);
        });
      });
    }
  }

  private EnvironmentCacheItem fillEnvironmentCacheItem(int count, DbEnvironment env, PublishAction publishAction) {
    final Set<DbApplicationFeature> features =
      new QDbApplicationFeature().parentApplication.eq(env.getParentApplication()).whenArchived.isNull().findSet();
    Map<UUID, DbFeatureValue> envFeatures =
      new QDbFeatureValue()
        .select(QDbFeatureValue.Alias.id, QDbFeatureValue.Alias.locked, QDbFeatureValue.Alias.feature.id,
          QDbFeatureValue.Alias.rolloutStrategies, QDbFeatureValue.Alias.version,
          QDbFeatureValue.Alias.defaultValue)
        .feature.whenArchived.isNull()
        .environment.eq(env).findStream()
        .collect(Collectors.toMap(f -> f.getFeature().getId(), Function.identity()));
    final Opts empty = Opts.empty();
    final EnvironmentCacheItem eci = new EnvironmentCacheItem()
      .action(publishAction)
      .environment(toEnvironment(env, features))
      .organizationId(env.getParentApplication().getPortfolio().getOrganization().getId())
      .portfolioId(env.getParentApplication().getPortfolio().getId())
      .applicationId(env.getParentApplication().getId())
      .featureValues(features.stream().map(f -> {
        final DbFeatureValue featureV = envFeatures.get(f.getId());
        final FeatureValue featureValue = convertUtils.toFeatureValue(f, featureV, empty);
        if (featureV != null) {
          featureValue.setRolloutStrategies(collectCombinedRolloutStrategies(featureV, f.getValueType()));
        }
        featureValue.setEnvironmentId(null);
        return featureValue;
      }).collect(Collectors.toList()))
      .serviceAccounts(env.getServiceAccountEnvironments().stream().map(s ->
        new ServiceAccount()
          .id(s.getServiceAccount().getId())
          ).collect(Collectors.toList()))
      .count(count);

    return eci;
  }

  private Environment toEnvironment(DbEnvironment env, Set<DbApplicationFeature> features) {
    // match these fields with the finder environmentsByCacheName so you don't get fields you don't need
    return new Environment()
      .version(env.getVersion())
      .id(env.getId())
      .name(env.getName())
      .features(features.stream()
        .map(ef -> convertUtils.toApplicationFeature(ef, Opts.empty()))
        .collect(Collectors.toList()));
  }

  private QDbEnvironment environmentsByCacheName(String cacheName) {
    return new QDbEnvironment()
      .select(QDbEnvironment.Alias.id, QDbEnvironment.Alias.name, QDbEnvironment.Alias.version)
      .whenArchived.isNull()
      .parentApplication.portfolio.organization.namedCache.cacheName.eq(cacheName)
      .environmentFeatures.feature.fetch()
      .parentApplication.fetch(QDbApplication.Alias.id)
      .parentApplication.portfolio.fetch(QDbPortfolio.Alias.id)
      .parentApplication.portfolio.organization.fetch(QDbOrganization.Alias.id);
  }

  @Override
  public void registerCache(String cacheName, CacheBroadcast cacheBroadcast) {
    cacheBroadcasters.put(
      cacheName, cacheBroadcast
    );
  }

  @Override
  public void publishFeatureChange(DbFeatureValue featureValue) {
    executor.submit(() -> {
      String cacheName = getFeatureValueCacheName(featureValue);
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        innerPublishFeatureValueChange(featureValue, cacheBroadcast);
      }
    });
  }

  public void innerPublishFeatureValueChange(DbFeatureValue featureValue, CacheBroadcast cacheBroadcast) {
    final FeatureValue value = convertUtils.toFeatureValue(featureValue, Opts.empty());
    final Feature feature = convertUtils.toFeature(featureValue);
    cacheBroadcast.publishFeature(
      new FeatureValueCacheItem()
        .feature(feature)
        .value(value)
        .strategies(collectCombinedRolloutStrategies(featureValue, feature.getValueType()))
        .environmentId(featureValue.getEnvironment().getId())
        .action(PublishAction.UPDATE));
  }

  // combines the custom and shared rollout strategies
  private List<RolloutStrategy> collectCombinedRolloutStrategies(DbFeatureValue featureValue, FeatureValueType type) {

    final List<DbStrategyForFeatureValue> activeSharedStrategies =
      new QDbStrategyForFeatureValue()
        .enabled.isTrue()
        .featureValue.eq(featureValue)
        .rolloutStrategy.whenArchived.isNull()
        .rolloutStrategy.fetch().findList();

    List<RolloutStrategy> allStrategies = new ArrayList<>();

    if (featureValue.getRolloutStrategies() != null) {
      allStrategies.addAll(featureValue.getRolloutStrategies());
    }

    allStrategies.addAll(
      activeSharedStrategies.stream().map(s -> {
        RolloutStrategy rs = s.getRolloutStrategy().getStrategy();

        rs.setName(null);
        rs.setColouring(null);
        rs.setAvatar(null);

        if (s.getValue() != null) {
          switch (type) {
            case BOOLEAN:
              rs.setValue(Boolean.parseBoolean(s.getValue()));
              break;
            case STRING:
            case JSON:
              rs.setValue(s.getValue());
              break;
            case NUMBER:
              rs.setValue(new BigDecimal(s.getValue()));
              break;
          }
        }

        return rs;
      }).collect(Collectors.toList()));

    return allStrategies;
  }

  @Override
  public void deleteFeatureChange(DbApplicationFeature feature, UUID environmentId) {
    executor.submit(() -> {
      String cacheName =
        new QDbNamedCache().organizations.portfolios.applications.eq(feature.getParentApplication()).findOne().getCacheName();
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

  // todo: consider caching using an LRU map
  private String getFeatureValueCacheName(DbFeatureValue strategy) {
    return new QDbNamedCache().organizations.portfolios.applications.environments.environmentFeatures.eq(strategy).findOne().getCacheName();
  }

  // this call comes in from the service layer
  @Override
  public void updateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction) {
    executor.submit(() -> internalUpdateServiceAccount(serviceAccount, publishAction));
  }

  private void internalUpdateServiceAccount(DbServiceAccount serviceAccount, PublishAction publishAction) {
    String cacheName =
      new QDbNamedCache().organizations.portfolios.serviceAccounts.id.eq(serviceAccount.getId()).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

    if (cacheName != null) {
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        if (publishAction != PublishAction.DELETE) {
          log.debug("Updating service account {} -> {}", serviceAccount.getId(), serviceAccount.getApiKeyServerEval());
          cacheBroadcast.publishServiceAccount(new ServiceAccountCacheItem()
            .count(serviceAccountsByCacheName(cacheName).findCount())
            .serviceAccount(fillServiceAccount(serviceAccount))
            .action(publishAction)
          );
        } else {
          log.info("ignored service account publish for delete");
        }
      } else {
        log.info("can't publish service account, no broadcaster for cache {}", cacheName);
      }
    } else {
      log.info("Can't published service account, no cache");
    }
  }

  @Override
  public void deleteServiceAccount(UUID id) {
    executor.submit(() -> internalDeleteServiceAccount(id));
  }

  private void internalDeleteServiceAccount(UUID id) {
    String cacheName =
      new QDbNamedCache().organizations.portfolios.serviceAccounts.id.eq(id).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

    if (cacheName != null) {
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        log.debug("Sending delete for service account `{}`", id);
        cacheBroadcast.publishServiceAccount(new ServiceAccountCacheItem()
          .count(serviceAccountsByCacheName(cacheName).findCount() - 1)  // now one less
          .serviceAccount(new ServiceAccount().id(id)) // just send the id, thats all the cache needs
          .action(PublishAction.DELETE)
        );
      }
    }
  }

  @Override
  public void updateEnvironment(DbEnvironment environment, PublishAction publishAction) {
    executor.submit(() -> internalUpdateEnvironment(environment, publishAction));
  }

  private void internalUpdateEnvironment(DbEnvironment environment, PublishAction publishAction) {
    if (environment != null) {
      String cacheName =
        new QDbNamedCache().organizations.portfolios.applications.environments.eq(environment).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

      if (cacheName != null) {
        CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

        if (cacheBroadcast != null) {
          log.info("publishing environment {} ({})", environment.getName(), environment.getId());
          final EnvironmentCacheItem environmentCacheItem =
            fillEnvironmentCacheItem(environmentsByCacheName(cacheName).findCount(), environment, publishAction);
          cacheBroadcast.publishEnvironment(environmentCacheItem);
        }
      }
    }
  }

  @Override
  public void deleteEnvironment(UUID id) {
    if (id != null) {
      String cacheName =
        new QDbNamedCache().organizations.portfolios.applications.environments.id.eq(id).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);
      if (cacheName != null) {
        CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

        if (cacheBroadcast != null) {
          log.debug("deleting environment: `{}`", id);
          cacheBroadcast.publishEnvironment(new EnvironmentCacheItem()
            .count(environmentsByCacheName(cacheName).findCount() - 1)
            .environment(new Environment().id(id))
            .action(PublishAction.DELETE));
        }
      }
    }
  }

  /**
   * unlike pushing out feature values one by one as they change, this can represent the deletion of a feature value
   * across the board.
   *
   * @param appFeature
   * @param action
   */
  private void publishAppLevelFeatureChange(DbApplicationFeature appFeature, PublishAction action) {
    String cacheName =
      new QDbNamedCache().organizations.portfolios.applications.eq(appFeature.getParentApplication()).findOneOrEmpty().map(DbNamedCache::getCacheName).orElse(null);

    if (cacheName != null) {
      CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

      if (cacheBroadcast != null) {
        Feature feature = convertUtils.toApplicationFeature(appFeature, Opts.empty());

        Map<UUID, DbFeatureValue> featureValues = new HashMap<>();

        if (action != PublishAction.DELETE) {
          // dont' care about values if deleting
          new QDbFeatureValue()
            .environment.whenArchived.isNull()
            .environment.parentApplication.eq(appFeature.getParentApplication())
            .feature.eq(appFeature).findEach(fe -> {
            featureValues.put(fe.getEnvironment().getId(), fe);
          });
        }

        new QDbEnvironment().parentApplication.eq(appFeature.getParentApplication()).whenArchived.isNull().findList().forEach(env -> {
          final FeatureValue featureValue = convertUtils.toFeatureValue(appFeature, featureValues.get(env.getId()),
            Opts.empty());
          cacheBroadcast.publishFeature(
            new FeatureValueCacheItem().feature(feature)
              .value(featureValue).environmentId(env.getId()).action(action));
        });
      }
    }
  }

  @Override
  public void publishFeatureChange(DbApplicationFeature appFeature, PublishAction action) {
    executor.submit(() -> publishAppLevelFeatureChange(appFeature, action)); // background as not going away
  }

  /**
   * This is triggered when a rollout strategy updates or is deleted. We need to find all attached feature values
   * and republish them.
   *
   * @param rs - the rollout strategy that changed
   */
  @Override
  public void publishRolloutStrategyChange(DbRolloutStrategy rs) {
    executor.submit(() -> {
      final List<DbFeatureValue> updatedValues =
        new QDbFeatureValue().sharedRolloutStrategies.rolloutStrategy.eq(rs).sharedRolloutStrategies.enabled.isTrue().findList();

      if (!updatedValues.isEmpty()) {
        String cacheName = getFeatureValueCacheName(updatedValues.get(0));
        // they are all the same application and
        // hence the same cache
        CacheBroadcast cacheBroadcast = cacheBroadcasters.get(cacheName);

        if (cacheBroadcast != null) {
          updatedValues.forEach(fv -> {
            executor.submit(() -> {
              innerPublishFeatureValueChange(fv, cacheBroadcast);
            });
          });
        }
      }
    });
  }
}
