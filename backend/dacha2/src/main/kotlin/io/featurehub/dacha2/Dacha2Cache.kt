package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheServiceAccountPermission
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.dacha.model.PublishFeatureValue
import io.featurehub.dacha.model.PublishServiceAccount
import io.featurehub.enricher.FeatureEnrichmentCache
import java.util.UUID

public const val DACHA_2_ENVIRONMENT_MISS_CACHE = "dacha2_environment_miss_cache_size"
public const val DACHA_2_SERVICE_ACCOUNT_MISS_CACHE = "dacha2_service_account_miss_cache_size"
public const val DACHA_2_PERMS_CACHE = "dacha2_perms_cache_size"
public const val DACHA_2_ENVIRONMENT_CACHE = "dacha2_environment_cache_size"
public const val DACHA_2_SERVICE_ACCOUNT_CACHE = "dacha2_service_account_cache_size"
public const val DACHA_2_SERVICE_ACCOUNT_KEY_CACHE = "dacha2_service_account_key_cache_size"
public const val DACHA_2_FEATURES_IN_CACHE = "dacha2_features_in_cache_size"
public const val DACHA_2_SERVICE_ACCOUNTS_FILTERING = "dacha2_filter_use"


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
  // used by delegator to ensure timer and anything else is reset
  fun closeCache()
}

abstract class Dacha2BaseCache : Dacha2Cache, FeatureEnrichmentCache

