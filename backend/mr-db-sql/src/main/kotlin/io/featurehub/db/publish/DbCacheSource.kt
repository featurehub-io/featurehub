package io.featurehub.db.publish

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.ebean.datasource.DataSourceConfig
import io.featurehub.dacha.model.*
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.db.services.Conversions
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.opentelemetry.context.Context
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors
import kotlin.collections.set

open class DbCacheSource @Inject constructor(private val convertUtils: Conversions,  dsConfig: DataSourceConfig) : CacheSource {
  private val executor: ExecutorService

  @ConfigKey("cache.pool-size")
  private var cachePoolSize: Int?
  private val cacheBroadcasters: MutableMap<String, CacheBroadcast> = HashMap()

  init {
    cachePoolSize = dsConfig.maxConnections / 2
    if (cachePoolSize!! < 1) {
      cachePoolSize = 1
    }
    DeclaredConfigResolver.resolve(this)
    log.info("Using maximum of {} connections to service request from Dacha", cachePoolSize)
    executor = executorService()
  }

  protected fun executorService() : ExecutorService {
    return Context.taskWrapping(Executors.newFixedThreadPool(cachePoolSize!!))
  }

  override fun publishToCache(cacheName: String) {
    val cacheBroadcast = cacheBroadcasters[cacheName]
    if (cacheBroadcast != null) {
      val saFuture = executor.submit { publishToCacheServiceAccounts(cacheName, cacheBroadcast) }
      val envFuture = executor.submit { publishCacheEnvironments(cacheName, cacheBroadcast) }
      try {
        saFuture.get()
        envFuture.get()
      } catch (e: Exception) {
        log.error("Failed to publish cache.", e)
      }
    }
  }

  private fun publishToCacheServiceAccounts(cacheName: String, cacheBroadcast: CacheBroadcast) {
    val saFinder = serviceAccountsByCacheName(cacheName)
    val count = saFinder.findCount()
    if (count == 0) {
      log.info("database has no service accounts, publishing empty cache indicator.")
      cacheBroadcast.publishServiceAccount(PublishServiceAccount().action(PublishAction.EMPTY).count(0))
    } else {
      log.info("publishing {} service accounts to cache {}", count, cacheName)
      saFinder.findEach { sa: DbServiceAccount ->
        val saci = PublishServiceAccount()
          .action(PublishAction.CREATE)
          .serviceAccount(fillServiceAccount(sa))
          .count(count)
        cacheBroadcast.publishServiceAccount(saci)
      }
    }
  }

  private fun fillServiceAccount(sa: DbServiceAccount): CacheServiceAccount {
    val serviceAccount = CacheServiceAccount()
      .id(sa.id)
      .version(sa.version)
      .apiKeyClientSide(sa.apiKeyClientEval)
      .apiKeyServerSide(sa.apiKeyServerEval)
      .permissions(
        QDbServiceAccountEnvironment()
          .select(QDbServiceAccountEnvironment.Alias.permissions)
          .serviceAccount.id.eq(sa.id)
          .environment.whenUnpublished.isNull
          .environment.whenArchived.isNull
          .environment.fetch(QDbEnvironment.Alias.id)
          .findStream().map { sap: DbServiceAccountEnvironment ->
            CacheServiceAccountPermission()
              .permissions(convertUtils.splitServiceAccountPermissions(sap.permissions))
              .environmentId(sap.environment.id)
          }.collect(Collectors.toList())
      )

    log.trace("service account publishing: {}", serviceAccount)

    return serviceAccount
  }

  private fun serviceAccountsByCacheName(cacheName: String): QDbServiceAccount {
    return QDbServiceAccount().whenArchived.isNull.portfolio.organization.namedCache.cacheName.eq(cacheName)
  }

  private fun publishCacheEnvironments(cacheName: String, cacheBroadcast: CacheBroadcast) {
    val count = environmentsByCacheName(cacheName, true).findCount()
    if (count == 0) {
      log.info("database has no environments, publishing empty environments indicator.")
      val empty = UUID.randomUUID()
      cacheBroadcast.publishEnvironment(PublishEnvironment()
        .environment(CacheEnvironment().id(empty).version(Long.MAX_VALUE))
        .organizationId(empty)
        .applicationId(empty)
        .portfolioId(empty)

        .action(PublishAction.EMPTY)
        .count(0))
    } else {
      log.info("publishing {} environments to cache {}", count, cacheName)
      environmentsByCacheName(cacheName, false).findEach { env: DbEnvironment ->
        executor.submit {
          val eci = fillEnvironmentCacheItem(count, env, PublishAction.CREATE)
          cacheBroadcast.publishEnvironment(eci)
        }
      }
    }
  }

  private fun fillEnvironmentCacheItem(
    count: Int,
    env: DbEnvironment,
    publishAction: PublishAction
  ): PublishEnvironment {
    try {
      log.trace("starting env: {} / {} - application features", env.name, env.id)
      // all the features for this environment in this application regardless of values
      val features = QDbApplicationFeature().whenArchived.isNull
        .parentApplication.environments.id.eq(env.id)
        .select(
          QDbApplicationFeature.Alias.id,
          QDbApplicationFeature.Alias.key,
          QDbApplicationFeature.Alias.valueType,
          QDbApplicationFeature.Alias.version
        ).findList().associate { it.id!! to it!! }.toMutableMap()

      val fvFinder = QDbFeatureValue()
        .select(
          QDbFeatureValue.Alias.id,
          QDbFeatureValue.Alias.locked,
          QDbFeatureValue.Alias.feature.id,
          QDbFeatureValue.Alias.rolloutStrategies,
          QDbFeatureValue.Alias.version,
          QDbFeatureValue.Alias.retired,
          QDbFeatureValue.Alias.defaultValue
        ).feature.whenArchived.isNull.feature.fetch(
          QDbApplicationFeature.Alias.id
        ).environment.whenArchived.isNull.environment.whenUnpublished.isNull.environment.eq(env)

      val eci = PublishEnvironment()
        .action(publishAction)
        .environment(toEnvironment(env))
        .organizationId(env.parentApplication.portfolio.organization.id)
        .portfolioId(env.parentApplication.portfolio.id)
        .applicationId(env.parentApplication.id)
        .featureValues(fvFinder.findList().map { fv: DbFeatureValue -> toCacheEnvironmentFeature(fv, features) })
        .serviceAccounts(
          QDbServiceAccount().select(QDbServiceAccount.Alias.id).serviceAccountEnvironments.environment.id.eq(env.id)
            .findStream().map { obj: DbServiceAccount -> obj.id }.collect(Collectors.toList())
        )
        .count(count)

      // now add in the remaining features with empty values
      features.values.forEach { feature: DbApplicationFeature ->
        eci.addFeatureValuesItem(
          CacheEnvironmentFeature().feature(toCacheFeature(feature))
        )
      }

      log.trace("publishing env: {} / {} - full body {}", env.name, env.id, eci)

      return eci
    } catch (e: Exception) {
      log.error("failed to publish", e)
      throw e
    }
  }

  private fun toCacheEnvironmentFeature(
    dfv: DbFeatureValue,
    features: MutableMap<UUID, DbApplicationFeature>
  ): CacheEnvironmentFeature {
    log.trace("cache-environment-feature")
    val feature = features[dfv.feature.id]
    features.remove(dfv.feature.id)
    return CacheEnvironmentFeature()
      .feature(toCacheFeature(feature!!))
      .value(toCacheFeatureValue(dfv, feature))
  }

  private fun toEnvironment(env: DbEnvironment): CacheEnvironment {
    // match these fields with the finder environmentsByCacheName, so you don't get fields you don't need
    val ce = CacheEnvironment()
      .id(env.id)
      .version(env.version)

    if (env.userEnvironmentInfo != null ) {
      if (ce.environmentInfo == null) {
        ce.environmentInfo = HashMap(env.userEnvironmentInfo)
      }
    }

    if (env.managementEnvironmentInfo != null) {
      if (ce.environmentInfo == null) {
        ce.environmentInfo = HashMap(env.managementEnvironmentInfo)
      } else {
        ce.environmentInfo?.putAll(env.managementEnvironmentInfo)
      }
    }

    return ce
  }

  private fun toCacheFeature(feature: DbApplicationFeature): CacheFeature {
    return CacheFeature()
      .id(feature.id)
      .key(feature.key)
      .version(feature.version)
      .valueType(feature.valueType)
  }

  private fun toCacheFeatureValue(dfv: DbFeatureValue?, feature: DbApplicationFeature): CacheFeatureValue? {
    return if (dfv == null) {
      null
    } else CacheFeatureValue()
      .id(dfv.id)
      .version(dfv.version)
      .value(featureValueAsObject(dfv.defaultValue, feature.valueType))
      .locked(dfv.isLocked)
      .rolloutStrategies(collectCombinedRolloutStrategies(dfv))
      .key(feature.key)
      .retired(dfv.retired)
  }

  private fun featureValueAsObject(value: String?, valueType: FeatureValueType): Any? {
    if (value == null) return null
    if (FeatureValueType.BOOLEAN == valueType) {
      return value.toBoolean()
    }
    if (FeatureValueType.JSON == valueType || FeatureValueType.STRING == valueType) {
      return value
    }

    return if (FeatureValueType.NUMBER == valueType) {
      value.toBigDecimal()
    } else null
  }

  private fun environmentsByCacheName(cacheName: String, wantCount: Boolean): QDbEnvironment {
    log.trace("environment by cache-name: {}", cacheName)
    val envQuery = QDbEnvironment()
      .whenArchived.isNull.whenUnpublished.isNull.parentApplication.portfolio.organization.namedCache.cacheName.eq(
        cacheName
      ).environmentFeatures.feature.fetch().parentApplication.fetch(
        QDbApplication.Alias.id
      ).parentApplication.portfolio.fetch(QDbPortfolio.Alias.id).parentApplication.portfolio.organization.fetch(
        QDbOrganization.Alias.id
      )

    return if (wantCount) {
      envQuery .select(
        QDbEnvironment.Alias.id,
      )
    } else {
      envQuery.select(
        QDbEnvironment.Alias.id,
        QDbEnvironment.Alias.name,
        QDbEnvironment.Alias.version,
        QDbEnvironment.Alias.userEnvironmentInfo,
        QDbEnvironment.Alias.managementEnvironmentInfo
      )
    }
  }

  override fun registerCache(cacheName: String, cacheBroadcast: CacheBroadcast) {
    cacheBroadcasters[cacheName] = cacheBroadcast
  }

  override fun publishFeatureChange(featureValue: DbFeatureValue) {
    executor.submit {
      val cacheName = getFeatureValueCacheName(featureValue)
      val cacheBroadcast = cacheBroadcasters[cacheName]
      cacheBroadcast?.let { innerPublishFeatureValueChange(featureValue, it) }
    }
  }

  private fun innerPublishFeatureValueChange(featureValue: DbFeatureValue, cacheBroadcast: CacheBroadcast) {
    cacheBroadcast.publishFeature(
      PublishFeatureValue()
        .feature(toCacheEnvironmentFeature(featureValue, mutableMapOf(Pair(featureValue.feature.id, featureValue.feature))))
        .environmentId(featureValue.environment.id)
        .action(PublishAction.UPDATE)
    )
  }

  private fun fromRolloutStrategyAttribute(rsa: RolloutStrategyAttribute): CacheRolloutStrategyAttribute {
    return CacheRolloutStrategyAttribute()
      .conditional(rsa.conditional!!)
      .values(rsa.values)
      .fieldName(rsa.fieldName!!)
      .type(rsa.type!!)
  }

  private fun fromRolloutStrategy(rs: RolloutStrategy): CacheRolloutStrategy {
    return CacheRolloutStrategy()
      .id(rs.id ?: "rs-id")
      .percentage(rs.percentage)
      .percentageAttributes(rs.percentageAttributes)
      .value(rs.value)
      .attributes(if (rs.attributes == null) ArrayList() else rs.attributes!!
        .stream().map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
        .collect(Collectors.toList()))
  }

  // combines the custom and shared rollout strategies
  private fun collectCombinedRolloutStrategies(featureValue: DbFeatureValue): List<CacheRolloutStrategy> {
    log.trace("cache combine strategies")

    val allStrategies: MutableList<CacheRolloutStrategy> = ArrayList()
    if (featureValue.rolloutStrategies != null) {
      allStrategies.addAll(
        featureValue.rolloutStrategies.stream().map { rs: RolloutStrategy -> fromRolloutStrategy(rs) }
          .collect(Collectors.toList()))
    }

    val activeSharedStrategies = QDbStrategyForFeatureValue()
      .select(QDbStrategyForFeatureValue.Alias.value)
      .featureValue.id.eq(featureValue.id)
      .enabled.isTrue
      .rolloutStrategy.fetch(QDbRolloutStrategy.Alias.strategy)
      .findList()

    allStrategies.addAll(activeSharedStrategies.stream()
      .map { shared ->
        val rs = fromRolloutStrategy(shared.rolloutStrategy.strategy)
        rs.value = shared.value // the value associated with the shared strategy is set here not in the strategy itself
        rs
      }
      .collect(Collectors.toList()))

    return allStrategies
  }

  override fun deleteFeatureChange(feature: DbApplicationFeature, environmentId: UUID) {
    executor.submit {
      val cacheName = QDbNamedCache().organizations.portfolios.applications.eq(feature.parentApplication)
        .findOne()!!.cacheName
      val cacheBroadcast = cacheBroadcasters[cacheName]
      cacheBroadcast?.publishFeature(
        PublishFeatureValue()
          .feature(
            CacheEnvironmentFeature()
              .feature(toCacheFeature(feature))
          )
          .environmentId(environmentId)
          .action(PublishAction.DELETE)
      )
    }
  }

  // todo: consider caching using an LRU map
  private fun getFeatureValueCacheName(strategy: DbFeatureValue): String {
    return QDbNamedCache().organizations.portfolios.applications.environments.environmentFeatures.eq(
      strategy
    ).findOne()!!.cacheName
  }

  // this call comes in from the service layer
  override fun updateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {
    executor.submit { internalUpdateServiceAccount(serviceAccount, publishAction) }
  }

  private fun internalUpdateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {
    val cacheName =
      QDbNamedCache().organizations.portfolios.serviceAccounts.id.eq(serviceAccount.id).findOneOrEmpty()
        .map { obj: DbNamedCache -> obj.cacheName }
        .orElse(null)
    if (cacheName != null) {
      val cacheBroadcast = cacheBroadcasters[cacheName]
      if (cacheBroadcast != null) {
        if (publishAction != PublishAction.DELETE) {
          log.debug("Updating service account {} -> {}", serviceAccount.id, serviceAccount.apiKeyServerEval)
          cacheBroadcast.publishServiceAccount(
            PublishServiceAccount()
              .count(serviceAccountsByCacheName(cacheName).findCount())
              .serviceAccount(fillServiceAccount(serviceAccount))
              .action(publishAction)
          )
        } else {
          log.info("ignored service account publish for delete")
        }
      } else {
        log.info("can't publish service account, no broadcaster for cache {}", cacheName)
      }
    } else {
      log.info("Can't published service account, no cache")
    }
  }

  override fun deleteServiceAccount(id: UUID) {
    executor.submit { internalDeleteServiceAccount(id) }
  }

  private fun internalDeleteServiceAccount(id: UUID) {
    val cacheName = QDbNamedCache().organizations.portfolios.serviceAccounts.id.eq(id).findOneOrEmpty()
      .map { obj: DbNamedCache -> obj.cacheName }
      .orElse(null)
    if (cacheName != null) {
      val cacheBroadcast = cacheBroadcasters[cacheName]
      if (cacheBroadcast != null) {
        log.debug("Sending delete for service account `{}`", id)
        cacheBroadcast.publishServiceAccount(
          PublishServiceAccount()
            .count(serviceAccountsByCacheName(cacheName).findCount() - 1) // now one less
            .serviceAccount(
              CacheServiceAccount()
                .id(id)
                .apiKeyServerSide("")
                .apiKeyClientSide("")
                .version(Long.MAX_VALUE)
            ) // just send the id, that is all the cache needs
            .action(PublishAction.DELETE)
        )
      }
    }
  }

  override fun updateEnvironment(environment: DbEnvironment, publishAction: PublishAction) {
    executor.submit { internalUpdateEnvironment(environment, publishAction) }
  }

  private fun internalUpdateEnvironment(environment: DbEnvironment?, publishAction: PublishAction) {
    if (environment != null) {
      val cacheName =
        QDbNamedCache().organizations.portfolios.applications.environments.eq(environment).findOneOrEmpty()
          .map { obj: DbNamedCache -> obj.cacheName }
          .orElse(null)
      if (cacheName != null) {
        val cacheBroadcast = cacheBroadcasters[cacheName]
        if (cacheBroadcast != null) {
          log.trace("publishing environment {} ({})", environment.name, environment.id)
          val environmentCacheItem = fillEnvironmentCacheItem(
            environmentsByCacheName(cacheName, true).findCount(),
            environment,
            publishAction
          )
          cacheBroadcast.publishEnvironment(environmentCacheItem)
        }
      }
    }
  }

  override fun deleteEnvironment(id: UUID) {
    val cacheName =
      QDbNamedCache().organizations.portfolios.applications.environments.id.eq(id).findOneOrEmpty()
        .map { obj: DbNamedCache -> obj.cacheName }
        .orElse(null)
    if (cacheName != null) {
      val cacheBroadcast = cacheBroadcasters[cacheName]
      if (cacheBroadcast != null) {
        log.debug("deleting environment: `{}`", id)
        val randomUUID = UUID.randomUUID() // we use this to fill in not-nullable fields
        cacheBroadcast.publishEnvironment(
          PublishEnvironment()
            .organizationId(randomUUID)
            .portfolioId(randomUUID)
            .applicationId(randomUUID)
            .count(environmentsByCacheName(cacheName, true).findCount() - 1)
            .environment(CacheEnvironment().id(id).version(Long.MAX_VALUE))
            .action(PublishAction.DELETE)
        )
      }
    }
  }

  /**
   * unlike pushing out feature values one by one as they change, this can represent the deletion of a feature value
   * across the board.
   */
  private fun publishAppLevelFeatureChange(appFeature: DbApplicationFeature, action: PublishAction, originalKey: String?) {
    val cacheName =
      QDbNamedCache().organizations.portfolios.applications.eq(appFeature.parentApplication).findOneOrEmpty()
        .map { obj: DbNamedCache -> obj.cacheName }
        .orElse(null)
    if (cacheName != null) {
      val cacheBroadcast = cacheBroadcasters[cacheName]
      if (cacheBroadcast != null) {
        val featureValues: MutableMap<UUID, DbFeatureValue> = HashMap()
        if (action != PublishAction.DELETE) {
          // dont' care about values if deleting
          QDbFeatureValue().environment.whenArchived.isNull.environment.whenUnpublished.isNull.environment.parentApplication.eq(
            appFeature.parentApplication
          ).feature.eq(appFeature).findEach { fe: DbFeatureValue -> featureValues[fe.environment.id] = fe }
        }
        val cacheFeature = toCacheFeature(appFeature)

        // deletes cause the key to change, so this restores it, SDKs should be using the ID in any case
        if (originalKey != null) {
          cacheFeature.key = originalKey
        }

        QDbEnvironment().parentApplication.eq(appFeature.parentApplication)
          .whenArchived.isNull
          .whenUnpublished.isNull.findList()
          .forEach { env: DbEnvironment ->
            val toCacheFeatureValue = toCacheFeatureValue(featureValues[env.id], appFeature)
            // deletes cause the key to change, so this restores it, SDKs should be using the ID in any case
            if (originalKey != null && toCacheFeatureValue != null) {
              toCacheFeatureValue.key = originalKey
            }
            cacheBroadcast.publishFeature(
                PublishFeatureValue()
                  .feature(
                    CacheEnvironmentFeature()
                      .feature(cacheFeature)
                      .value(toCacheFeatureValue)
                  )
                  .environmentId(env.id).action(action)
              )
            }
      }
    }
  }

  override fun publishFeatureChange(appFeature: DbApplicationFeature, action: PublishAction) {
    executor.submit { publishAppLevelFeatureChange(appFeature, action, null) } // background as not going away
  }

  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction, featureKey: String) {
    executor.submit { publishAppLevelFeatureChange(appFeature, update, featureKey) } // background as not going away
  }

  /**
   * This is triggered when a rollout strategy updates or is deleted. We need to find all attached feature values
   * and republish them.
   *
   * @param rs - the rollout strategy that changed
   */
  override fun publishRolloutStrategyChange(rs: DbRolloutStrategy) {
    executor.submit {
      val updatedValues =
        QDbFeatureValue().sharedRolloutStrategies.rolloutStrategy.eq(rs).sharedRolloutStrategies.enabled.isTrue.findList()
      if (updatedValues.isNotEmpty()) {
        val cacheName = getFeatureValueCacheName(updatedValues[0])
        // they are all the same application and
        // hence the same cache
        val cacheBroadcast = cacheBroadcasters[cacheName]
        if (cacheBroadcast != null) {
          updatedValues.forEach { fv: DbFeatureValue ->
            executor.submit {
              innerPublishFeatureValueChange(
                fv,
                cacheBroadcast
              )
            }
          }
        }
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(DbCacheSource::class.java)
  }
}
