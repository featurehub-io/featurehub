package io.featurehub.dacha2

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.RemovalListener
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

class InvalidKeyException : RuntimeException()

open class Dacha2NewCacheImpl @Inject constructor(private val mrDacha2Api: Dacha2ServiceClient,
                                               private val featureValueFactory: FeatureValuesFactory) : Dacha2BaseCache() {
  protected val serviceAccountApiKeyCache: LoadingCache<String, CacheServiceAccount>
  protected val serviceAccountCache: Cache<UUID, CacheServiceAccount>
  protected val serviceAccountMissCache: Cache<String, Boolean>
  protected val environmentCache: LoadingCache<UUID, EnvironmentFeatures>

  // any environment id misses get put into here
  protected val environmentMissCache: Cache<UUID, Boolean>

  // environment id, environment-features
  protected val permsCache: Cache<String, CacheServiceAccountPermission>

  private var maximumServiceAccountMisses = FallbackPropertyConfig.getConfig("dacha2.cache.service-account.miss-size", "10000").toLong()

  private var maximumServiceAccountPermissionsSize = FallbackPropertyConfig.getConfig("dacha2.cache.service-account.perms-size", "10000").toLong()

  private var maximumServiceAccounts = FallbackPropertyConfig.getConfig("dacha2.cache.service-account.size", "10000").toLong()

  private var maximumEnvironments = FallbackPropertyConfig.getConfig("dacha2.cache.environment.size", "10000").toLong()

  private var maximumEnvironmentMisses = FallbackPropertyConfig.getConfig("dacha2.cache.environment.miss-size", "10000").toLong()

  private var cacheStreamedUpdates: Boolean = FallbackPropertyConfig.getConfig("dacha2.cache.all-updates") != "false"

  var apiKey: String? = FallbackPropertyConfig.getConfig("dacha2.cache.api-key")
  var resettingCache: Boolean = false
  private val metricTimer = Timer()

  init {
    environmentMissCache = Caffeine.newBuilder()
      .maximumSize(maximumEnvironmentMisses)
      .build()

    serviceAccountMissCache = Caffeine.newBuilder()
      .maximumSize(maximumServiceAccountMisses)
      .build()

    permsCache = Caffeine.newBuilder()
      .maximumSize(maximumServiceAccountPermissionsSize)
      .build()

    environmentCache = Caffeine.newBuilder()
      .maximumSize(maximumEnvironments)
      .removalListener(RemovalListener<UUID, EnvironmentFeatures> { envId, eFeatures, _ ->
        if (envId == null || eFeatures == null || resettingCache) return@RemovalListener
        permsCache.invalidateAll(eFeatures.env.serviceAccounts.map { "$envId/$it" })
      })
      .build(CacheLoader { id: UUID ->
        try {
          val env = mrDacha2Api.getEnvironment(id, apiKey).env
          gaugeFeaturesInCache.inc(env.featureValues.size.toDouble())
          featureValueFactory.create(env)
        } catch (nfe: NotFoundException) {
          log.trace("environment id {} does not exist", id)
          environmentMissCache.put(id, true)
          throw nfe
        } catch (e: Exception) {
          log.error("failed", e)
          throw e
        }
      })

    serviceAccountCache = Caffeine.newBuilder().maximumSize(maximumServiceAccounts / 2).build()

    serviceAccountApiKeyCache = Caffeine.newBuilder()
      .maximumSize(maximumServiceAccounts)
      .removalListener(RemovalListener<String, CacheServiceAccount> { key, value, _ ->
        if (key == null || value == null || resettingCache) return@RemovalListener
        permsCache.invalidateAll(value.permissions.map { permCacheKey(it.environmentId, key) })
      })
      .build(CacheLoader { key: String ->
        try {
          val serviceAccount = mrDacha2Api.getServiceAccount(key, apiKey).serviceAccount
          fillServiceAccountCache(key, serviceAccount)
          serviceAccountCache.put(serviceAccount.id, serviceAccount)
          serviceAccount
        } catch (nfe: NotFoundException) {
          log.trace("service account {} does not exist", key)
          serviceAccountMissCache.put(key, true)
          throw nfe
        }
      })

    // there is no real other way to keep them up to date that isn't very complex and very inaccurate
    log.info("[dacha2] starting metric timer for cache size")
    metricTimer.schedule(object : TimerTask() {
      override fun run() {
        resetMetricCounters()
      }
    }, 5000, 5000)
  }

  override fun closeCache() {
    log.info("[dacha2] shutting down metric timer for cache size")
    metricTimer.cancel()
  }

  fun resetMetricCounters() {
    gaugePermsCache.set(permsCache.estimatedSize().toDouble())
    gaugeEnvironmentCache.set(environmentCache.estimatedSize().toDouble())

    gaugeServiceAccountCache.set(serviceAccountCache.estimatedSize().toDouble())
    gaugeServiceAccountKeyCache.set(serviceAccountApiKeyCache.estimatedSize().toDouble())

    gaugeEnvironmentMissCache.set(environmentMissCache.estimatedSize().toDouble())
    gaugeServiceAccountMissCache.set(serviceAccountMissCache.estimatedSize().toDouble())
  }

  fun resetCache() {
    if (!resettingCache) {
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
          val serviceAccount = serviceAccountApiKeyCache.get(apiKey)
          val result = serviceAccount.permissions.find { it.environmentId == eId }
          if (result == null) {
            log.trace("Unable to find environment id {} in serviceAccount", serviceAccount)
            throw InvalidKeyException()
          }
          result
        } catch (_: Exception) {
          if (log.isTraceEnabled) {
            log.trace("failed to get service account permission {}/{}", eId, apiKey)
          }
          throw InvalidKeyException()
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
      val found = environmentCache.getIfPresent(envId)
      if (found != null) {
        environmentCache.invalidate(envId)
        gaugeFeaturesInCache.dec(found.featureCount.toDouble())
      }

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
        gaugeFeaturesInCache.dec(oldEnv.featureCount.toDouble())
        val newEnv = EnvironmentFeatures(env)
        gaugeFeaturesInCache.inc(newEnv.featureCount.toDouble())
        environmentCache.put(envId, newEnv)
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

          serviceAccountApiKeyCache.invalidateAll(listOf(it.apiKeyServerSide, it.apiKeyClientSide))
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
        permsCache.invalidateAll(existing.permissions.takeWhile { envs[it.environmentId] == null }.flatMap {
          listOf(
            permCacheKey(it.environmentId, sa.apiKeyServerSide),
            permCacheKey(it.environmentId, sa.apiKeyClientSide)
          )
        })
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

  companion object {
    val log: Logger = LoggerFactory.getLogger(Dacha2CacheImpl::class.java)

    val gaugeServiceAccountMissCache = MetricsCollector.gauge(DACHA_2_SERVICE_ACCOUNT_MISS_CACHE, "Requests for service accounts that don't exist")
    val gaugeServiceAccountCache = MetricsCollector.gauge(DACHA_2_SERVICE_ACCOUNT_CACHE, "The size of cache for service accounts")
    val gaugeServiceAccountKeyCache = MetricsCollector.gauge(DACHA_2_SERVICE_ACCOUNT_KEY_CACHE, "The size of cache for service account keys (2x service accounts)")

    val gaugePermsCache = MetricsCollector.gauge(DACHA_2_PERMS_CACHE, "The size of cache for permissions for a service account")

    val gaugeEnvironmentMissCache = MetricsCollector.gauge(DACHA_2_ENVIRONMENT_MISS_CACHE, "Requests for environments that don't exist")
    val gaugeEnvironmentCache = MetricsCollector.gauge(DACHA_2_ENVIRONMENT_CACHE, "The size of cache for environments")

    val gaugeFeaturesInCache = MetricsCollector.gauge(DACHA_2_FEATURES_IN_CACHE, "How many features are in this cache")
    val counterFilterUse = MetricsCollector.counter(DACHA_2_SERVICE_ACCOUNTS_FILTERING, "How many times filtering has been used")
  }
}

