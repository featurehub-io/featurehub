package io.featurehub.dacha2

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.cache.RemovalListener
import io.featurehub.dacha.model.CacheServiceAccount
import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishAction
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.enricher.EnrichmentEnvironment
import io.featurehub.enricher.FeatureEnrichmentCache
import io.featurehub.metrics.MetricsCollector
import io.featurehub.utils.FallbackPropertyConfig
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


data class FeatureCollection(
  val features: FeatureValues,
  val perms: CacheServiceAccountPermission,
  val serviceAccountId: UUID
)

interface Dacha2Cache {
  // we specifically DON'T want it implementing the Dacha2CacheListener interface
  fun updateServiceAccount(serviceAccount: PublishServiceAccount)
  fun updateEnvironment(env: PublishEnvironment)
  fun updateFeature(feature: PublishFeatureValue)

  fun getFeatureCollection(eId: UUID, apiKey: String): FeatureCollection?

  /**
   * This will throw an exception if it can't be found
   */
  fun findEnvironment(eId: UUID): FeatureValues
  fun enableCache(cacheEnable: Boolean)
}

abstract class Dacha2BaseCache : Dacha2Cache, FeatureEnrichmentCache {}

class Dacha2DelegatingCache @Inject constructor(private val mrDacha2Api: Dacha2ServiceClient,
                                                private val featureValueFactory: FeatureValuesFactory) : Dacha2BaseCache() {
  private var cache: Dacha2BaseCache
  private var cacheEnabled: Boolean = false
  private val log: Logger = LoggerFactory.getLogger(Dacha2DelegatingCache::class.java)

  init {
    cache = Dacha2PassthroughImpl(mrDacha2Api, featureValueFactory)
    log.info("started dacha2 in uncached passthrough mode (waiting on confirmed connection to streaming layer)")
  }

  override fun updateServiceAccount(serviceAccount: PublishServiceAccount) {
    cache.updateServiceAccount(serviceAccount)
  }

  override fun updateEnvironment(env: PublishEnvironment) {
    cache.updateEnvironment(env)
  }

  override fun updateFeature(feature: PublishFeatureValue) {
    cache.updateFeature(feature)
  }

  override fun getFeatureCollection(eId: UUID, apiKey: String): FeatureCollection? {
    return cache.getFeatureCollection(eId, apiKey)
  }

  override fun findEnvironment(eId: UUID): FeatureValues {
    return cache.findEnvironment(eId)
  }

  override fun enableCache(cacheEnable: Boolean) {
    if (cacheEnabled && !cacheEnable) {
      log.info("lost connectivity, swapping dacha2 in uncached passthrough mode")
      cache = Dacha2PassthroughImpl(mrDacha2Api, featureValueFactory)
    } else if (!cacheEnabled && cacheEnable) {
      log.info("connectivity to streaming established, swapping dacha2 to cached mode (empty cache)")
      cache = Dacha2CacheImpl(mrDacha2Api, featureValueFactory)
    }

    cacheEnabled = cacheEnable
  }

  override fun getEnrichableEnvironment(eId: UUID): EnrichmentEnvironment {
    return cache.getEnrichableEnvironment(eId)
  }
}

class Dacha2PassthroughImpl(private val mrDacha2Api: Dacha2ServiceClient, private val featureValueFactory: FeatureValuesFactory) : Dacha2BaseCache() {
  private val apiKey = FallbackPropertyConfig.getConfig("dacha2.cache.api-key")

  override fun updateServiceAccount(serviceAccount: PublishServiceAccount) {
  }

  override fun updateEnvironment(env: PublishEnvironment) {
  }

  override fun updateFeature(feature: PublishFeatureValue) {
  }

  override fun getEnrichableEnvironment(eId: UUID): EnrichmentEnvironment {
    val envFeatures = findEnvironment(eId)
    return EnrichmentEnvironment(envFeatures.getFeatures(), envFeatures.environment)
  }

  private fun getEnvironment(eId: UUID): EnvironmentFeatures {
    val env = mrDacha2Api.getEnvironment(eId, apiKey).env
    return featureValueFactory.create(env)
  }

  private fun getServiceAccount(key: String): CacheServiceAccount {
    return mrDacha2Api.getServiceAccount(key, apiKey).serviceAccount
  }

  override fun getFeatureCollection(eId: UUID, apiKey: String): FeatureCollection? {
    val sa = getServiceAccount(apiKey)

    var collection: FeatureCollection? = null

    sa.permissions.find { it.environmentId == eId }?.let { perms ->
      if (perms.permissions.isNotEmpty()) {
        val features = getEnvironment(eId)
        collection = if (sa.filters != null && sa.filters!!.isNotEmpty()) {
          FeatureCollection(FilteredEnvironmentFeatures(features, sa.filters!!), perms, sa.id)
        } else {
          FeatureCollection(features, perms, sa.id)
        }
      }
    }

    return collection
  }

  override fun findEnvironment(eId: UUID): FeatureValues {
    return getEnvironment(eId)
  }

  override fun enableCache(cacheEnable: Boolean) {
  }
}

private const val DACHA_2_ENVIRONMENT_MISS_CACHE = "dacha2-environment-miss-cache-size"
private const val DACHA_2_SERVICE_ACCOUNT_MISS_CACHE = "dacha2-service-account-miss-cache-size"
private const val DACHA_2_PERMS_CACHE = "dacha2-perms-cache-size"
private const val DACHA_2_ENVIRONMENT_CACHE = "dacha2-environment-cache-size"
private const val DACHA_2_SERVICE_ACCOUNT_CACHE = "dacha2-service-account-cache-size"
private const val DACHA_2_SERVICE_ACCOUNT_KEY_CACHE = "dacha2-service-account-key-cache-size"
private const val DACHA_2_FEATURES_IN_CACHE = "dacha2-features-in-cache-size"
private const val DACHA_2_SERVICE_ACCOUNTS_FILTERING = "dacha2-filter-use"

open class Dacha2CacheImpl @Inject constructor(private val mrDacha2Api: Dacha2ServiceClient,
                                               private val featureValueFactory: FeatureValuesFactory) : Dacha2BaseCache() {
  private val log: Logger = LoggerFactory.getLogger(Dacha2CacheImpl::class.java)
  protected val serviceAccountApiKeyCache: LoadingCache<String, CacheServiceAccount>
  protected val serviceAccountCache: Cache<UUID, CacheServiceAccount>
  protected val serviceAccountMissCache: Cache<String, Boolean>
  protected val environmentCache: LoadingCache<UUID, EnvironmentFeatures>

  // any environment id misses get put into here
  protected val environmentMissCache: Cache<UUID, Boolean>

  // environment id, environment-features
  protected val permsCache: Cache<String, CacheServiceAccountPermission>

  val gaugeServiceAccountMissCache = MetricsCollector.gauge(DACHA_2_SERVICE_ACCOUNT_MISS_CACHE, "Requests for service accounts that don't exist")
  val gaugeServiceAccountCache = MetricsCollector.gauge(DACHA_2_SERVICE_ACCOUNT_CACHE, "The size of cache for service accounts")
  val gaugeServiceAccountKeyCache = MetricsCollector.gauge(DACHA_2_SERVICE_ACCOUNT_KEY_CACHE, "The size of cache for service account keys (2x service accounts)")

  val gaugePermsCache = MetricsCollector.gauge(DACHA_2_PERMS_CACHE, "The size of cache for permissions for a service account")

  val gaugeEnvironmentMissCache = MetricsCollector.gauge(DACHA_2_ENVIRONMENT_MISS_CACHE, "Requests for environments that don't exist")
  val gaugeEnvironmentCache = MetricsCollector.gauge(DACHA_2_ENVIRONMENT_CACHE, "The size of cache for environments")

  val gaugeFeaturesInCache = MetricsCollector.gauge(DACHA_2_FEATURES_IN_CACHE, "How many features are in this cache")
  val counterFilterUse = MetricsCollector.counter(DACHA_2_SERVICE_ACCOUNTS_FILTERING, "How many times filtering has been used")

  private var maximumServiceAccountMisses = FallbackPropertyConfig.getConfig("dacha2.cache.service-account.miss-size", "10000").toLong()

  private var maximumServiceAccountPermissionsSize = FallbackPropertyConfig.getConfig("dacha2.cache.service-account.perms-size", "10000").toLong()

  private var maximumServiceAccounts = FallbackPropertyConfig.getConfig("dacha2.cache.service-account.size", "10000").toLong()

  private var maximumEnvironments = FallbackPropertyConfig.getConfig("dacha2.cache.environment.size", "10000").toLong()

  private var maximumEnvironmentMisses = FallbackPropertyConfig.getConfig("dacha2.cache.environment.miss-size", "10000").toLong()

  private var cacheStreamedUpdates: Boolean = FallbackPropertyConfig.getConfig("dacha2.cache.all-updates") != "false"

  var apiKey: String? = FallbackPropertyConfig.getConfig("dacha2.cache.api-key")
  var resettingCache: Boolean = false

  init {
    environmentMissCache = CacheBuilder.newBuilder()
      .maximumSize(maximumEnvironmentMisses)
      .build()

    serviceAccountMissCache = CacheBuilder.newBuilder()
      .maximumSize(maximumServiceAccountMisses)
      .build()

    permsCache = CacheBuilder.newBuilder()
      .maximumSize(maximumServiceAccountPermissionsSize)
      .build()

    environmentCache = CacheBuilder.newBuilder()
      .maximumSize(maximumEnvironments)
      .removalListener(RemovalListener<UUID, EnvironmentFeatures> { notification ->
        val eFeatures = notification.value!!
        val envId = notification.key!!

        if (!resettingCache) {
          gaugeFeaturesInCache.dec(eFeatures.featureCount.toDouble())
          permsCache.invalidateAll(eFeatures.env.serviceAccounts.map { "$envId/$it" })
        }
      })
      .build(object : CacheLoader<UUID, EnvironmentFeatures>() {
        override fun load(id: UUID): EnvironmentFeatures {
          try {
            val env = mrDacha2Api.getEnvironment(id, apiKey).env
            gaugeFeaturesInCache.inc(env.featureValues.size.toDouble())

            return featureValueFactory.create(env)
          } catch (nfe: NotFoundException) {
            log.trace("environment id {} does not exist", id)
            environmentMissCache.put(id, true)
            throw nfe
          } catch (e: Exception) {
            log.error("failed", e)
            throw e
          }
        }
      })

    serviceAccountCache = CacheBuilder.newBuilder().maximumSize(maximumServiceAccounts / 2).build()

    serviceAccountApiKeyCache = CacheBuilder.newBuilder()
      .maximumSize(maximumServiceAccounts)
      .removalListener(RemovalListener<String, CacheServiceAccount> { notification ->
        val value = notification.value!!


        if (!resettingCache) {
          // this is cached per service account api key, it has its own removal listener
          permsCache.invalidateAll(value.permissions.map {
            permCacheKey(it.environmentId, notification.key!!)
          })
        }
      })
      .build(object : CacheLoader<String, CacheServiceAccount>() {
        override fun load(key: String): CacheServiceAccount {
          try {
            val serviceAccount = mrDacha2Api.getServiceAccount(key, apiKey).serviceAccount
            // every SA is actually two entries
            fillServiceAccountCache(key, serviceAccount)

            serviceAccountCache.put(serviceAccount.id, serviceAccount)
            return serviceAccount
          } catch (nfe: NotFoundException) {
            log.trace("service account {} does not exist", key)
            serviceAccountMissCache.put(key, true)
            throw nfe
          }
        }
      })

    // there is no real other way to keep them up to date that isn't very complex and very inaccurate
    val metricTimer = Timer()
    metricTimer.schedule(object : TimerTask() {
      override fun run() {
        resetMetricCounters()
      }
    }, 5000, 5000)


  }

  fun resetMetricCounters() {
    gaugePermsCache.set(permsCache.size().toDouble())
    gaugeEnvironmentCache.set(environmentCache.size().toDouble())

    gaugeServiceAccountCache.set(serviceAccountCache.size().toDouble())
    gaugeServiceAccountKeyCache.set(serviceAccountApiKeyCache.size().toDouble())

    gaugeEnvironmentMissCache.set(environmentMissCache.size().toDouble())
    gaugeServiceAccountMissCache.set(serviceAccountMissCache.size().toDouble())
  }

  /**
   * this causes us to double-load the api keys in, which is what we want
   */
  fun fillServiceAccountCache(key: String, serviceAccount: CacheServiceAccount) {
    if (serviceAccount.apiKeyClientSide == key) {
      serviceAccountApiKeyCache.put(serviceAccount.apiKeyServerSide, serviceAccount)
    } else {
      serviceAccountApiKeyCache.put(serviceAccount.apiKeyClientSide, serviceAccount)
    }
  }

  private fun permCacheKey(eId: UUID, apiKey: String) = "${eId}/${apiKey}"

  override fun getFeatureCollection(eId: UUID, apiKey: String): FeatureCollection? {
    // if the environment is already in the sin-bin, return not found
    environmentMissCache.getIfPresent(eId)?.let {
      log.trace("environmentMissCache: {}", eId)
      return null
    }

    // same with api key
    serviceAccountMissCache.getIfPresent(apiKey)?.let {
      log.trace("serviceMissCache: {}", apiKey)
      return null
    }

    val comboKey = permCacheKey(eId, apiKey)
    try {
      val perms = permsCache.get(comboKey) {
        try {
          // this can cause an exception
          val serviceAccount = serviceAccountApiKeyCache.get(apiKey)

          val result = serviceAccount.permissions.find { it.environmentId == eId }
          if (result == null) {
            log.trace("Unable to find environment id {} in serviceAccount", serviceAccount)
          }
          result
        } catch (e: Exception) {
          if (log.isTraceEnabled) {
            log.trace("failed to get service account permission {}/{}", eId, apiKey)
          }
          // this will cause an exception
          null
        }
      }

      if (perms.permissions.isEmpty()) {
        log.trace("no permissions for this service account  {}/{}", perms, comboKey)
        return null
      }

      // accessing the environment-cache can cause an exception
      val serviceAccount = serviceAccountApiKeyCache.get(apiKey)

      if (serviceAccount.filters != null && serviceAccount.filters!!.isNotEmpty()) {
        counterFilterUse.inc()
        return FeatureCollection(FilteredEnvironmentFeatures(environmentCache[eId], serviceAccount.filters!!), perms, serviceAccount.id)
      }

      return FeatureCollection(environmentCache[eId], perms, serviceAccount.id)
    } catch (e: Exception) {
      log.trace("could not find in perms cache {}", comboKey, e)
      return null
    }
  }

  override fun findEnvironment(eId: UUID): FeatureValues {
    environmentMissCache.getIfPresent(eId)?.let {
      throw RuntimeException()
    }

    return environmentCache.get(eId)
  }

  override fun enableCache(cacheEnable: Boolean) {
  }

  fun isEnvironmentPresent(eId: UUID): Boolean {
    environmentMissCache.getIfPresent(eId)?.let {
      return false
    }

    return environmentCache.getIfPresent(eId) != null
  }

  override fun updateEnvironment(env: PublishEnvironment) {
    if (env.action == PublishAction.EMPTY) return

    val envId = env.environment.id

    if (env.action == PublishAction.DELETE) {
      environmentCache.invalidate(envId)

      environmentMissCache.put(envId, true)
      return
    }

    var created = false
    val oldEnv = if (cacheStreamedUpdates) {
      environmentCache.get(envId) {
        created = true
        EnvironmentFeatures(env)
      }
    } else {
      environmentCache.getIfPresent(envId)
    }

    environmentMissCache.invalidate(envId)

    // if we don't have it and it is new, lets ignore it as no-one has asked for it
    if (oldEnv == null) return
    // if we created it, its in the cache now
    if (!created) {
      if (env.environment.version >= oldEnv.environment.environment.version) {
        log.trace("environment {} is same version or newer (incoming {}, old {}), storing", envId, env.environment.version, oldEnv.environment.environment.version)
        environmentCache.put(envId, EnvironmentFeatures(env))
      } else if (log.isTraceEnabled) {
        log.trace(
          "received old update to environment {} (current {} vs incoming {}) - ignoring",
          envId,
          oldEnv.environment.environment.version,
          env.environment.version
        )
      }
    }
  }

  override fun updateServiceAccount(serviceAccount: PublishServiceAccount) {
    if (serviceAccount.action == PublishAction.EMPTY || serviceAccount.serviceAccount == null ) return

    serviceAccount.serviceAccount?.let {sa ->
      val sId = sa.id

      if (serviceAccount.action == PublishAction.DELETE) {
        // if we have it, update respective caches   and just return
        serviceAccountCache.getIfPresent(sId)?.let {

          serviceAccountApiKeyCache.invalidate(listOf(it.apiKeyServerSide, it.apiKeyClientSide))
          serviceAccountMissCache.put(it.apiKeyClientSide, true)
          serviceAccountMissCache.put(it.apiKeyServerSide, true)

          serviceAccountCache.invalidate(sId)
        }

        return
      } // if a delete, deal with and return

      var created = false
      val existing = if (cacheStreamedUpdates) {
        serviceAccountCache.get(sId) {
          created = true
          sa
        }
      } else serviceAccountCache.getIfPresent(sId)

      if (existing == null) {
        return // we throw it away as we are not caching streamed updates
      }

      if (created) { // it was new so it is a new update and we are caching
        // just in case we had these keys in the miss cache
        serviceAccountMissCache.invalidateAll(listOf(sa.apiKeyServerSide, sa.apiKeyClientSide))

        // its already in serviceAccountCache, so we just have to put it in the ApiKey cache
        serviceAccountApiKeyCache.put(sa.apiKeyServerSide, sa)
        serviceAccountApiKeyCache.put(sa.apiKeyClientSide, sa)
      } else if (sa.version >= existing.version) { // it was already in there, checking if its newer
        serviceAccountCache.put(sId, sa) // its equal or newer, we have to store it

        // checking if the API keys changed, and if so, invalidating and caching respectively
        if (existing.apiKeyServerSide != sa.apiKeyServerSide) {
          serviceAccountMissCache.put(existing.apiKeyServerSide, true)
          serviceAccountApiKeyCache.invalidate(existing.apiKeyServerSide)
        }
        if (existing.apiKeyClientSide != sa.apiKeyClientSide) {
          serviceAccountMissCache.put(existing.apiKeyClientSide, true)
          serviceAccountApiKeyCache.invalidate(existing.apiKeyClientSide)
        }

        serviceAccountMissCache.invalidateAll(listOf(sa.apiKeyServerSide, sa.apiKeyClientSide))
        stashServiceAccount(sa, sId)

        val envs = sa.permissions.associateBy { it.environmentId }

        // take while the envId in the existing list doesn't exist in the new one, map it to a list of
        // perm cache keys we have to remove, flatten, invalidate
        permsCache.invalidateAll(existing.permissions.takeWhile { envs[it.environmentId] == null }.map {
          listOf(permCacheKey(it.environmentId, sa.apiKeyServerSide), permCacheKey(it.environmentId, sa.apiKeyClientSide)) }
           .flatten())
      } else {
        log.trace("attempted to update {} with older service account {}", existing, sa)
      }
    }
  }

  private fun stashServiceAccount(sa: CacheServiceAccount, sId: UUID) {
    serviceAccountApiKeyCache.put(sa.apiKeyServerSide, sa)
    serviceAccountApiKeyCache.put(sa.apiKeyClientSide, sa)
    serviceAccountCache.put(sId, sa)
  }

  private fun receivedNewFeatureForExistingEnvironmentFeatureCache(feature:  PublishFeatureValue, environment: EnvironmentFeatures) {
    if (feature.action == PublishAction.CREATE || feature.action == PublishAction.UPDATE) {
      if (log.isTraceEnabled) {
        log.trace("received new feature `{}` for environment `{}`", feature.feature.feature.key, environment.environment.environment.id)
      }

      if (feature.feature.value != null) {
        environment.setFeatureValue(feature.feature)
      } else {
        environment.setFeature(feature.feature)
      }
    }
  }

  override fun getEnrichableEnvironment(eId: UUID): EnrichmentEnvironment {
    val envFeatures = findEnvironment(eId)
    return EnrichmentEnvironment(envFeatures.getFeatures(), envFeatures.environment)
  }

  override fun updateFeature(feature: PublishFeatureValue) {
    if (log.isTraceEnabled) {
      log.trace("feature update: {}", feature)
    }

    if (feature.action == PublishAction.EMPTY) {
      return
    }

    // we ignore features we don't have the environment for
    environmentCache.getIfPresent(feature.environmentId)?.let { ef ->
      val newFeature = feature.feature.feature

      if (feature.action == PublishAction.DELETE) {
        ef.remove(newFeature.id)
        log.trace("removed feature")
        return
      }

      val newFeatureValue = feature.feature.value

      val existingCachedFeature = ef[newFeature.id]

      // new feature entirely, so skip out
      if (existingCachedFeature == null) {
        log.trace("received feature we didn't know about before")
        receivedNewFeatureForExistingEnvironmentFeatureCache(feature, ef)
        return
      }

      // now we know it is an update to an existing feature (maybe)
      val existingFeature = existingCachedFeature.feature
      val existingValue = existingCachedFeature.value

      if (feature.action == PublishAction.CREATE || feature.action == PublishAction.UPDATE) {
        if (existingFeature.version < newFeature.version) {
          log.trace("feature itself updated (old {} new {}), storing the updated feature", existingFeature.version, newFeature.version)
          ef.setFeature(feature.feature)
        }

        if (newFeatureValue != null) {  // values once set are never null as we cannot support versioning if so
          if (existingValue == null || existingValue.version < newFeatureValue.version) {
            log.trace("feature value updated, storing new value")
            ef.setFeatureValue(feature.feature)
            return
          } else if (existingValue.version == newFeatureValue.version) {
            log.trace("feature value didn't change, ignoring")
            return // just ignore it
          } else {
            log.trace("feature value was old,  ignoring as existing is {} vs incoming {}", existingValue, newFeatureValue)
          }
        }
      }
    } ?: log.debug("received update for unknown feature {}: {}", feature.environmentId, feature.feature.feature.key)
  }
}

class Dacha2DumpOnReconnectCache @Inject constructor(mrDacha2Api: Dacha2ServiceClient,
                                 featureValueFactory: FeatureValuesFactory) : Dacha2CacheImpl(mrDacha2Api, featureValueFactory) {
  private var cacheEnabled = true
  private val log: Logger = LoggerFactory.getLogger(Dacha2DumpOnReconnectCache::class.java)

  init {
    log.info("Dacha2 - using dump on streaming layer reconnect")
  }

  override fun enableCache(cacheEnable: Boolean) {
    // if we are being enabled and we were "disabled", we dump the cache on this situation
    if (cacheEnable && !cacheEnabled) {
      log.info("streaming has reconnected, dumping cache as assuming potential poisoning")

      try {
        resettingCache = true
        serviceAccountApiKeyCache.invalidateAll()
        serviceAccountCache.invalidateAll()
        serviceAccountMissCache.invalidateAll()
        environmentCache.invalidateAll()
        environmentMissCache.invalidateAll()
        permsCache.invalidateAll()
        gaugeFeaturesInCache.set(0.0)
      } finally {
        resettingCache = false
      }
    }

    if (!cacheEnable) {
      log.info("streaming layer has gone away, continuning to serve from cache and will drop cache when reconnected")
    }

    cacheEnabled = cacheEnable
  }
}
