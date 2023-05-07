package io.featurehub.dacha2

import cd.connect.app.config.ConfigKey
import com.google.common.cache.*
import io.featurehub.dacha.model.*
import io.featurehub.dacha2.api.Dacha2ServiceClient
import io.featurehub.enricher.EnrichmentEnvironment
import io.featurehub.enricher.FeatureEnrichmentCache
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
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
}

class Dacha2CacheImpl @Inject constructor(private val mrDacha2Api: Dacha2ServiceClient,
                                          private val featureValueFactory: FeatureValuesFactory) : Dacha2Cache,
  FeatureEnrichmentCache {
  private val log: Logger = LoggerFactory.getLogger(Dacha2CacheImpl::class.java)
  private val serviceAccountApiKeyCache: LoadingCache<String, CacheServiceAccount>
  private val serviceAccountCache: Cache<UUID, CacheServiceAccount>
  private val serviceAccountMissCache: Cache<String, Boolean>
  private val environmentCache: LoadingCache<UUID, EnvironmentFeatures>

  // any environment id misses get put into here
  private val environmentMissCache: Cache<UUID, Boolean>

  // environment id, environment-features
  private val permsCache: Cache<String, CacheServiceAccountPermission>

  @ConfigKey("dacha2.cache.service-account.miss-size")
  var maximumServiceAccountMisses: Long? = 10000

  @ConfigKey("dacha2.cache.service-account.perms-size")
  var maximumServiceAccountPermissionsSize: Long? = 10000

  @ConfigKey("dacha2.cache.service-account.size")
  var maximumServiceAccounts: Long? = 10000

  @ConfigKey("dacha2.cache.environment.size")
  var maximumEnvironments: Long? = 10000

  @ConfigKey("dacha2.cache.environment.miss-size")
  var maximumEnvironmentMisses: Long? = 10000

  @ConfigKey("dacha2.cache.all-updates")
  var cacheStreamedUpdates: Boolean? = true

  @ConfigKey("dacha2.cache.api-key")
  var apiKey: String? = null

  init {
    environmentMissCache = CacheBuilder.newBuilder()
      .maximumSize(maximumEnvironmentMisses!!)
      .build()

    serviceAccountMissCache = CacheBuilder.newBuilder()
      .maximumSize(maximumServiceAccountMisses!!)
      .build()

    permsCache = CacheBuilder.newBuilder()
      .maximumSize(maximumServiceAccountPermissionsSize!!)
      .build()

    environmentCache = CacheBuilder.newBuilder()
      .maximumSize(maximumEnvironments!!)
      .build(object : CacheLoader<UUID, EnvironmentFeatures>() {
        override fun load(id: UUID): EnvironmentFeatures {
          try {
            val env = mrDacha2Api.getEnvironment(id, apiKey).env
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


    serviceAccountCache = CacheBuilder.newBuilder().maximumSize(maximumServiceAccounts!! / 2).build()

    serviceAccountApiKeyCache = CacheBuilder.newBuilder()
      .maximumSize(maximumServiceAccounts!!)
      .removalListener(RemovalListener<String, CacheServiceAccount> { notification ->
        val value = notification.value!!

        serviceAccountCache.invalidate(value)

        permsCache.invalidateAll(value.permissions.map {
          permCacheKey(it.environmentId, notification.key!!)
        })
      })
      .build(object : CacheLoader<String, CacheServiceAccount>() {
        override fun load(key: String): CacheServiceAccount {
          try {
            val serviceAccount = mrDacha2Api.getServiceAccount(key, apiKey).serviceAccount
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
      return null
    }

    // same with api key
    serviceAccountMissCache.getIfPresent(apiKey)?.let {
      return null
    }

    val comboKey = permCacheKey(eId, apiKey)
    try {
      val perms = permsCache.get(comboKey) {
        try {
          // this can cause an exception
          val serviceAccount = serviceAccountApiKeyCache.get(apiKey)

          serviceAccount.permissions.find { it.environmentId == eId }
        } catch (e: Exception) {
          // this will cause an exception
          null
        }
      }

      if (perms.permissions.isEmpty()) {
        return null
      }

      // accessing the environment-cache can cause an exception
      return FeatureCollection(environmentCache[eId], perms, serviceAccountApiKeyCache.get(apiKey).id)
    } catch (e: Exception) {
      return null
    }
  }

  override fun findEnvironment(eId: UUID): FeatureValues {
    environmentMissCache.getIfPresent(eId)?.let {
      throw RuntimeException()
    }

    return environmentCache.get(eId)
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
      permsCache.invalidateAll(env.serviceAccounts.map { "$envId/$it" })
      environmentMissCache.put(envId, true)
      return
    }

    val oldEnv = environmentCache.getIfPresent(envId)
    if (oldEnv == null) {
      // we know it exists now so if it was deleted and added to the miss cache, removed it
      environmentMissCache.invalidate(envId)
      if (cacheStreamedUpdates!!) {
        environmentCache.put(envId, EnvironmentFeatures(env))
      }
    } else if (env.environment.version >= oldEnv.environment.environment.version) {
      environmentMissCache.invalidate(envId)
      environmentCache.put(envId, EnvironmentFeatures(env))
    }
  }

  override fun updateServiceAccount(serviceAccount: PublishServiceAccount) {
    if (serviceAccount.action == PublishAction.EMPTY || serviceAccount.serviceAccount == null ) return

    serviceAccount.serviceAccount?.let {sa ->
      val sId = sa.id

      if (serviceAccount.action == PublishAction.DELETE) {
        // if we have it, update respective caches
        serviceAccountCache.getIfPresent(sId)?.let {
          serviceAccountApiKeyCache.invalidate(listOf(it.apiKeyServerSide, it.apiKeyClientSide))
          serviceAccountMissCache.put(it.apiKeyClientSide, true)
          serviceAccountMissCache.put(it.apiKeyServerSide, true)
          serviceAccountCache.invalidate(sId)
        }

        return
      }

      val existing = serviceAccountCache.getIfPresent(sId)
      if (existing == null) {
        serviceAccountMissCache.invalidateAll(listOf(sa.apiKeyServerSide, sa.apiKeyClientSide))
        if (cacheStreamedUpdates!!) {
          stashServiceAccount(sa, sId)
        }
      } else if (sa.version >= existing.version) {
        serviceAccountMissCache.invalidateAll(listOf(sa.apiKeyServerSide, sa.apiKeyClientSide))
        stashServiceAccount(sa, sId)

        val envs = sa.permissions.associateBy { it.environmentId }

        // take while the envId in the existing list doesn't exist in the new one, map it to a list of
        // perm cache keys we have to remove, flatten, invalidate
        permsCache.invalidateAll(existing.permissions.takeWhile { envs[it.environmentId] == null }.map {
          listOf(permCacheKey(it.environmentId, sa.apiKeyServerSide), permCacheKey(it.environmentId, sa.apiKeyClientSide)) }
           .flatten())
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

      environment.setFeature(feature.feature)
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

      val newValue = feature.feature.value

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
          log.trace("feature itself updated, storing the updated feature")
          ef.setFeature(feature.feature)
        }

        if (newValue != null) {  // values once set are never null as we cannot support versioning if so
          if (existingValue == null || existingValue.version < newValue.version) {
            log.trace("feature value updated, storing new value")
            ef.setFeatureValue(feature.feature)
            return
          } else if (existingValue.version == newValue.version) {
            log.trace("feature value didn't change, ignoring")
            return // just ignore it
          } else {
            log.trace("feature value situation unknown existing {} vs new {}", existingValue, newValue)
          }
        }

        log.warn("attempting to remove a feature older than we have")
      }
    } ?: log.debug("received update for unknown feature {}: {}", feature.environmentId, feature.feature.feature.key)
  }
}
