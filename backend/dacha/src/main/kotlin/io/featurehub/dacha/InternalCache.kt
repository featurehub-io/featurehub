package io.featurehub.dacha

import io.featurehub.dacha.model.*
import java.util.*
import java.util.stream.Stream

interface InternalCache {
  interface FeatureValues {
    val features: Collection<CacheEnvironmentFeature>
    val environment: PublishEnvironment
    val etag: String
  }

  class FeatureCollection(
    val features: FeatureValues,
    val perms: CacheServiceAccountPermission,
    val serviceAccountId: UUID
  )

  /**
   * Is this cache complete and ready for requests?
   */
  fun cacheComplete(): Boolean

  /*
 * Register an action to complete when the cache is complete
 */
  fun onCompletion(notify: Runnable?)
  fun clear()
  fun environments(): Stream<PublishEnvironment>?
  fun serviceAccounts(): Stream<PublishServiceAccount>?
  fun getFeaturesByEnvironmentAndServiceAccount(environmentId: UUID, apiKey: String): FeatureCollection?
  fun findEnvironment(environmentId: UUID): PublishEnvironment?
  fun updateServiceAccount(sa: PublishServiceAccount)
  fun updateEnvironment(e: PublishEnvironment)
  fun updateFeatureValue(fv: PublishFeatureValue)

}
