package io.featurehub.db.publish

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.ebean.datasource.DataSourceConfig
import io.featurehub.dacha.model.*
import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.db.model.*
import io.featurehub.db.model.query.*
import io.featurehub.db.services.Conversions
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.common.CacheSource
import io.featurehub.mr.events.dacha2.CacheApi
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import io.featurehub.utils.ExecutorSupplier
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.annotation.PostConstruct
import jakarta.inject.Inject
import org.glassfish.hk2.api.IterableProvider
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.*
import java.util.stream.Collectors


/**
 * This allows us to have multiple active broadcasters
 */
internal class CacheBroadcastProxy(
  private val broadcasters: List<CacheBroadcast>,
  private val executor: ExecutorService
) : CacheBroadcast {

  override fun publishEnvironment(eci: PublishEnvironment) {
    broadcasters.forEach { cb ->
      executor.submit { cb.publishEnvironment(eci) }
    }
  }

  override fun publishServiceAccount(saci: PublishServiceAccount) {
    broadcasters.forEach { cb ->
      executor.submit { cb.publishServiceAccount(saci) }
    }
  }

  override fun publishFeatures(features: PublishFeatureValues) {
    broadcasters.forEach { cb ->
      executor.submit { cb.publishFeatures(features) }
    }
  }
}


open class DbCacheSource @Inject constructor(
  private val convertUtils: Conversions, dsConfig: DataSourceConfig,
  private val cacheBroadcasters: IterableProvider<CacheBroadcast>,
  private val internalFeatureGroupApi: CacheSourceFeatureGroupApi,
  private val featureModelWalker: FeatureModelWalker,
  executorSupplier: ExecutorSupplier
) : CacheSource, CacheApi, CacheRefresherApi {
  private val executor: ExecutorService

  @ConfigKey("cache.pool-size")
  private var cachePoolSize: Int?
  private var cacheBroadcast: CacheBroadcast

  val featureGroupsEnabled: Boolean

  init {
    featureGroupsEnabled = "true".equals(
      FallbackPropertyConfig.getConfig(
        "feature-groups.enabled", "true"
      ), ignoreCase = true)

    cachePoolSize = dsConfig.maxConnections / 2
    if (cachePoolSize!! < 1) {
      cachePoolSize = 1
    }
    DeclaredConfigResolver.resolve(this)
    log.info("Using maximum of {} connections to service request from Dacha", cachePoolSize)
    executor = executorSupplier.executorService(cachePoolSize!!)
    cacheBroadcast = CacheBroadcastProxy(emptyList(), executor)
  }

  @PostConstruct
  fun publishInit() = if (cacheBroadcasters.size == 1) {
    cacheBroadcast = cacheBroadcasters.first()
  } else {
    cacheBroadcast = CacheBroadcastProxy(cacheBroadcasters.toList(), executor)
  }

  override fun publishObjectsAssociatedWithCache() {
    val saFuture = executor.submit { publishToCacheServiceAccounts(cacheBroadcast) }
    val envFuture = executor.submit { publishCacheEnvironments(cacheBroadcast) }
    try {
      saFuture.get()
      envFuture.get()
    } catch (e: Exception) {
      log.error("Failed to publish cache.", e)
    }
  }

  private fun publishToCacheServiceAccounts(cacheBroadcast: CacheBroadcast) {
    val saFinder = allServiceAccounts()
    val count = saFinder.findCount()
    if (count == 0) {
      log.info("database has no service accounts, publishing empty cache indicator.")
      cacheBroadcast.publishServiceAccount(PublishServiceAccount().action(PublishAction.EMPTY).count(0))
    } else {
      log.info("publishing {} service accounts", count)
      saFinder.findEach { sa: DbServiceAccount ->
        val saci = PublishServiceAccount()
          .action(PublishAction.CREATE)
          .serviceAccount(fillServiceAccount(sa))
          .count(count)
        cacheBroadcast.publishServiceAccount(saci)
      }
    }
  }

  private fun findServiceAccount(key: String): DbServiceAccount? {
    var finder = QDbServiceAccount()

    finder = if (key.contains('*')) {
      finder.apiKeyClientEval.eq(key)
    } else {
      finder.apiKeyServerEval.eq(key)
    }

    return finder.select(
      QDbServiceAccount.Alias.id,
      QDbServiceAccount.Alias.version,
      QDbServiceAccount.Alias.apiKeyClientEval,
      QDbServiceAccount.Alias.apiKeyServerEval,
    ).findOne()
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
              .permissions(convertUtils.splitServiceAccountPermissions(sap.permissions) ?: listOf())
              .environmentId(sap.environment.id)
          }.collect(Collectors.toList())
      )

    log.trace("service account publishing: {}", serviceAccount)

    return serviceAccount
  }

  private fun allServiceAccounts(): QDbServiceAccount {
    return QDbServiceAccount().whenArchived.isNull
  }

  private fun publishCacheEnvironments(cacheBroadcast: CacheBroadcast) {
    val count = allEnvironments(true).findCount()
    if (count == 0) {
      log.info("database has no environments, publishing empty environments indicator.")
      val empty = UUID.randomUUID()
      cacheBroadcast.publishEnvironment(
        PublishEnvironment()
          .environment(CacheEnvironment().id(empty).version(Long.MAX_VALUE))
          .organizationId(empty)
          .applicationId(empty)
          .portfolioId(empty)

          .action(PublishAction.EMPTY)
          .count(0)
      )
    } else {
      log.info("publishing {} environments", count)
      allEnvironments(false).findEach { env: DbEnvironment ->
        executor.submit {
          val eci = fillEnvironmentCacheItem(count, env, PublishAction.CREATE)
          cacheBroadcast.publishEnvironment(eci)
        }
      }
    }
  }

  private fun addSelectorToFeatureValue(finder: QDbFeatureValue): QDbFeatureValue {
    return finder.select(
      QDbFeatureValue.Alias.id,
      QDbFeatureValue.Alias.locked,
      QDbFeatureValue.Alias.feature.id,
      QDbFeatureValue.Alias.rolloutStrategies,
      QDbFeatureValue.Alias.version,
      QDbFeatureValue.Alias.retired,
      QDbFeatureValue.Alias.defaultValue,
      QDbFeatureValue.Alias.whoUpdated.id,
    ).feature.whenArchived.isNull.feature.fetch(
      QDbApplicationFeature.Alias.id
    )
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

      val featureGroupRolloutStrategy = if (featureGroupsEnabled) internalFeatureGroupApi.collectStrategiesFromGroupsForEnvironment(env.id) else mapOf()

      val fvFinder = addSelectorToFeatureValue(QDbFeatureValue())
        .environment.whenArchived.isNull.environment.whenUnpublished.isNull.environment.eq(env)

      val eci = PublishEnvironment()
        .action(publishAction)
        .environment(toEnvironment(env))
        .organizationId(env.parentApplication.portfolio.organization.id)
        .portfolioId(env.parentApplication.portfolio.id)
        .applicationId(env.parentApplication.id)
        .featureValues(fvFinder.findList().map { fv: DbFeatureValue -> toCacheEnvironmentFeature(fv, features, featureGroupRolloutStrategy[fv.feature.id]) })
        .serviceAccounts(
          QDbServiceAccount().select(QDbServiceAccount.Alias.id).serviceAccountEnvironments.environment.id.eq(env.id)
            .findList().map { obj: DbServiceAccount -> obj.id }
        )
        .count(count)

      // now add in the remaining features with empty values
      features.values.forEach { feature: DbApplicationFeature ->
        val toCacheFeature = toCacheFeature(feature)
        eci.addFeatureValuesItem(
          CacheEnvironmentFeature().feature(toCacheFeature)
            .featureProperties(featureModelWalker.walk(feature, null, toCacheFeature, null, null))
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
    features: MutableMap<UUID, DbApplicationFeature>,
    featureGroupRolloutStrategies: List<RolloutStrategy>?
  ): CacheEnvironmentFeature {
    log.trace("cache-environment-feature")
    val feature = features[dfv.feature.id]
    features.remove(dfv.feature.id)
    val toCacheFeature = toCacheFeature(feature!!)
    val toCacheFeatureValue = toCacheFeatureValue(dfv, feature, featureGroupRolloutStrategies)

    return CacheEnvironmentFeature()
      .feature(toCacheFeature)
      .value(toCacheFeatureValue)
      .featureProperties(featureModelWalker.walk(feature, dfv, toCacheFeature, toCacheFeatureValue, featureGroupRolloutStrategies))
  }

  // we should select out only the details we need to publish
  private fun findEnvironment(id: UUID): DbEnvironment? {
    return QDbEnvironment().id.eq(id).select(
      QDbEnvironment.Alias.id,
      QDbEnvironment.Alias.version,
      QDbEnvironment.Alias.userEnvironmentInfo,
      QDbEnvironment.Alias.managementEnvironmentInfo,
      QDbEnvironment.Alias.webhookEnvironmentInfo
    ).findOne()
  }

  private fun toEnvironment(env: DbEnvironment): CacheEnvironment {
    // match these fields with the finder environmentsByCacheName, so you don't get fields you don't need
    val ce = CacheEnvironment()
      .id(env.id)
      .version(env.version)

    if (env.userEnvironmentInfo != null) {
      ce.environmentInfo.putAll(env.userEnvironmentInfo)
    }

    if (env.managementEnvironmentInfo != null) {
      ce.environmentInfo.putAll(env.managementEnvironmentInfo)
    }

    ce.webhookEnvironment = env.webhookEnvironmentInfo

    return ce
  }

  private fun toCacheFeature(feature: DbApplicationFeature): CacheFeature {
    return CacheFeature()
      .id(feature.id)
      .key(feature.key)
      .version(feature.version)
      .valueType(feature.valueType)
  }

  private fun toCacheFeatureValue(dfv: DbFeatureValue?, feature: DbApplicationFeature, featureGroupRolloutStrategies: List<RolloutStrategy>?): CacheFeatureValue? {
    return if (dfv == null) {
      null
    } else CacheFeatureValue()
      .id(dfv.id!!)
      .version(dfv.version!!)
      .value(featureValueAsObject(dfv.defaultValue, feature.valueType))
      .locked(dfv.isLocked)
      .rolloutStrategies(collectCombinedRolloutStrategies(dfv, featureGroupRolloutStrategies))
      .key(feature.key)
      .retired(dfv.retired)
      .personIdWhoChanged(dfv.whoUpdated.id)
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

  private fun allEnvironments(wantCount: Boolean): QDbEnvironment {
    val envQuery = QDbEnvironment()
      .whenArchived.isNull.whenUnpublished.isNull.environmentFeatures.feature.fetch().parentApplication.fetch(
        QDbApplication.Alias.id
      ).parentApplication.portfolio.fetch(QDbPortfolio.Alias.id).parentApplication.portfolio.organization.fetch(
        QDbOrganization.Alias.id
      )

    return if (wantCount) {
      envQuery.select(
        QDbEnvironment.Alias.id,
      )
    } else {
      envQuery.select(
        QDbEnvironment.Alias.id,
        QDbEnvironment.Alias.name,
        QDbEnvironment.Alias.version,
        QDbEnvironment.Alias.userEnvironmentInfo,
        QDbEnvironment.Alias.managementEnvironmentInfo,
        QDbEnvironment.Alias.webhookEnvironmentInfo
      )
    }
  }

  override fun publishFeatureChange(featureValue: DbFeatureValue) {
    executor.submit {
      try {
        innerPublishFeatureValueChange(featureValue, cacheBroadcast)
      } catch (e: Exception) {
        log.error("Failed to publish", e)
      }
    }
  }

  private fun innerPublishFeatureValueChange(
    featureValue: DbFeatureValue,
    cacheBroadcast: CacheBroadcast
  ) {
    val featureGroupRolloutStrategy =  if (featureGroupsEnabled) internalFeatureGroupApi.collectStrategiesFromGroupsForEnvironmentFeature(
      featureValue.environment.id,
      featureValue.feature.id) else listOf()

    cacheBroadcast.publishFeatures(
      PublishFeatureValues().addFeaturesItem(
        PublishFeatureValue()
          .feature(
            toCacheEnvironmentFeature(
              featureValue,
              mutableMapOf(featureValue.feature.id to featureValue.feature),
              featureGroupRolloutStrategy
            )
          )
          .environmentId(featureValue.environment.id)
          .action(PublishAction.UPDATE)
      )
    )
  }

  private fun fromRolloutStrategyAttribute(rsa: RolloutStrategyAttribute): CacheRolloutStrategyAttribute {
    return CacheRolloutStrategyAttribute()
      .conditional(rsa.conditional)
      .values(rsa.values)
      .fieldName(rsa.fieldName)
      .type(rsa.type)
  }



  private fun fromRolloutStrategy(rs: RolloutStrategy): CacheRolloutStrategy {
    return CacheRolloutStrategy()
      .id(rs.id ?: "rs-id")
      .percentage(rs.percentage)
      .percentageAttributes(rs.percentageAttributes)
      .value(rs.value)
      .attributes(if (rs.attributes == null) mutableListOf() else rs.attributes!!
        .map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
        )
  }

  private fun fromApplicationRolloutStrategy(rs: DbStrategyForFeatureValue): CacheRolloutStrategy {
    return CacheRolloutStrategy()
      .id(rs.rolloutStrategy.shortUniqueCode)
      .percentage(rs.rolloutStrategy.strategy.percentage)
      .percentageAttributes(rs.rolloutStrategy.strategy.percentageAttributes)
      .value(rs.value)
      .attributes(if (rs.rolloutStrategy.strategy.attributes == null) mutableListOf() else rs.rolloutStrategy.strategy.attributes!!
        .map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
        )
  }

  // combines the custom and shared rollout strategies
  private fun collectCombinedRolloutStrategies(
    featureValue: DbFeatureValue,
    featureGroupRolloutStrategies: List<RolloutStrategy>?
  ): List<CacheRolloutStrategy> {
    log.trace("cache combine strategies")

    val allStrategies = mutableListOf<CacheRolloutStrategy>()
    allStrategies.addAll(featureValue.rolloutStrategies.map { rs -> fromRolloutStrategy(rs) })

    val activeSharedStrategies = QDbStrategyForFeatureValue()
      .select(QDbStrategyForFeatureValue.Alias.value)
      .featureValue.id.eq(featureValue.id)
      .enabled.isTrue
      .rolloutStrategy.fetch(QDbApplicationRolloutStrategy.Alias.strategy, QDbApplicationRolloutStrategy.Alias.shortUniqueCode)
      .findList()

    allStrategies.addAll(activeSharedStrategies.filter { !it.rolloutStrategy.strategy.disabled }.map { shared ->
        val rs = fromApplicationRolloutStrategy(shared)
        rs.value = shared.value // the value associated with the shared strategy is set here not in the strategy itself
        rs
      })

    featureGroupRolloutStrategies?.let { fgStrategies ->
      allStrategies.addAll(fgStrategies.map { fromRolloutStrategy(it) })
    }

    return allStrategies
  }

  override fun deleteFeatureChange(feature: DbApplicationFeature, environmentId: UUID) {
    executor.submit {
      cacheBroadcast.publishFeatures(
        PublishFeatureValues().addFeaturesItem(
          PublishFeatureValue()
            .feature(
              CacheEnvironmentFeature()
                .feature(toCacheFeature(feature))
            )
            .environmentId(environmentId)
            .action(PublishAction.DELETE)
        )
      )
    }
  }

  // this call comes in from the service layer
  override fun updateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {
    executor.submit { internalUpdateServiceAccount(serviceAccount, publishAction) }
  }

  private fun internalUpdateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {
    if (publishAction != PublishAction.DELETE) {
      log.debug("Updating service account {} -> {}", serviceAccount.id, serviceAccount.apiKeyServerEval)
      cacheBroadcast.publishServiceAccount(
        PublishServiceAccount()
          .count(allServiceAccounts().findCount())
          .serviceAccount(fillServiceAccount(serviceAccount))
          .action(publishAction)
      )
    } else {
      log.info("can't publish service account, no broadcaster")
    }
  }


  override fun deleteServiceAccount(id: UUID) {
    executor.submit { internalDeleteServiceAccount(id) }
  }

  private fun internalDeleteServiceAccount(id: UUID) {
    log.debug("Sending delete for service account `{}`", id)
    cacheBroadcast.publishServiceAccount(
      PublishServiceAccount()
        .count(allServiceAccounts().findCount() - 1) // now one less
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

  override fun updateEnvironment(environment: DbEnvironment, publishAction: PublishAction) {
    executor.submit { internalUpdateEnvironment(environment, publishAction) }
  }

  private fun internalUpdateEnvironment(environment: DbEnvironment?, publishAction: PublishAction) {
    if (environment != null) {
      log.trace("publishing environment {} ({})", environment.name, environment.id)
      val environmentCacheItem = fillEnvironmentCacheItem(
        allEnvironments(true).findCount(),
        environment,
        publishAction
      )
      cacheBroadcast.publishEnvironment(environmentCacheItem)
    }
  }

  override fun deleteEnvironment(id: UUID) {
    log.debug("deleting environment: `{}`", id)
    val randomUUID = UUID.randomUUID() // we use this to fill in not-nullable fields
    cacheBroadcast.publishEnvironment(
      PublishEnvironment()
        .organizationId(randomUUID)
        .portfolioId(randomUUID)
        .applicationId(randomUUID)
        .count(allEnvironments(true).findCount() - 1)
        .environment(CacheEnvironment().id(id).version(Long.MAX_VALUE))
        .action(PublishAction.DELETE)
    )
  }

  /**
   * unlike pushing out feature values one by one as they change, this can represent the deletion of a feature value
   * across the board.
   */
  private fun publishAppLevelFeatureChange(
    appFeature: DbApplicationFeature,
    action: PublishAction,
    originalKey: String?
  ) {
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
        val featureGroupStrategies = if (featureGroupsEnabled) internalFeatureGroupApi.collectStrategiesFromGroupsForEnvironmentFeature(env.id, appFeature.id) else listOf()
        val dfv = featureValues[env.id]
        val toCacheFeatureValue = toCacheFeatureValue(dfv, appFeature, featureGroupStrategies)
        // deletes cause the key to change, so this restores it, SDKs should be using the ID in any case
        if (originalKey != null && toCacheFeatureValue != null) {
          toCacheFeatureValue.key = originalKey
        }

        cacheBroadcast.publishFeatures(
          PublishFeatureValues().addFeaturesItem(
            PublishFeatureValue()
              .feature(
                CacheEnvironmentFeature()
                  .feature(cacheFeature)
                  .value(toCacheFeatureValue)
                  .featureProperties(featureModelWalker.walk(
                    appFeature,
                    dfv,
                    cacheFeature,
                    toCacheFeatureValue,
                    featureGroupStrategies
                  ))
              )
              .environmentId(env.id).action(action)
          )
        )
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
  override fun publishApplicationRolloutStrategyChange(action: PublishAction, rs: DbApplicationRolloutStrategy) {
    executor.submit {
      val updatedValues =
        addSelectorToFeatureValue(QDbFeatureValue())
          .sharedRolloutStrategies.rolloutStrategy.eq(rs)
          .sharedRolloutStrategies.enabled.isTrue.findList()

      if (updatedValues.isNotEmpty()) {
        // they are all the same application and
        // hence the same cache
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

  companion object {
    private val log = LoggerFactory.getLogger(DbCacheSource::class.java)
  }

  override fun getEnvironment(id: UUID): PublishEnvironment? {
    return findEnvironment(id)?.let {
      fillEnvironmentCacheItem(0, it, PublishAction.CREATE)
    }
  }

  override fun getServiceAccount(apiKey: String): CacheServiceAccount? {
    return findServiceAccount(apiKey)?.let {
      fillServiceAccount(it)
    }
  }

  override fun refreshPortfolios(portfolioIds: List<UUID>) {
    val count = allEnvironments(true).findCount()
    allEnvironments(false).parentApplication.portfolio.id.`in`(portfolioIds).findEach {
      executor.submit {
        val eci = fillEnvironmentCacheItem(count, it, PublishAction.UPDATE)
        cacheBroadcast.publishEnvironment(eci)
      }
    }

    val saCount = allServiceAccounts().findCount()

    allServiceAccounts().portfolio.id.`in`(portfolioIds).findEach {
      executor.submit {
        cacheBroadcast.publishServiceAccount(
          PublishServiceAccount()
            .count(saCount)
            .serviceAccount(fillServiceAccount(it))
            .action(PublishAction.UPDATE)
        )
      }
    }
  }

  override fun refreshApplications(applicationIds: List<UUID>) {
    val count = allEnvironments(true).findCount()
    allEnvironments(false).parentApplication.id.`in`(applicationIds).findEach {
      executor.submit {
        val eci = fillEnvironmentCacheItem(count, it, PublishAction.UPDATE)
        cacheBroadcast.publishEnvironment(eci)
      }
    }
  }

  override fun refreshEntireDatabase() {
    publishObjectsAssociatedWithCache()
  }
}
