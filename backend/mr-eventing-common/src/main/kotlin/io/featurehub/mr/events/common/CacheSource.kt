package io.featurehub.mr.events.common

import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.model.*
import java.util.*

interface CacheSource {

  /**
   * publish all environments, service accounts and features associated with this cache
   */
  fun publishObjectsAssociatedWithCache()

  fun deleteFeatureChange(feature: DbApplicationFeature, environmentId: UUID)

  /**
   * Whenever a service account changes or at start up, publish it
   */
  fun updateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction)

  fun deleteServiceAccount(id: UUID)

  /**
   * whenever an environment changes or at start up, publish it.
   *
   * This is used to cache environments and their feature values.
   */
  fun updateEnvironment(environment: DbEnvironment, publishAction: PublishAction)

  fun deleteEnvironment(id: UUID)


  fun publishFeatureChange(appFeature: DbApplicationFeature, action: PublishAction)

  /*
   * This one is used by the Archive to preserve the original feature key
   */
  fun publishFeatureChange(
      appFeature: DbApplicationFeature, update: PublishAction,
      featureKey: String
  )

  /**
   * Whenever a feature value changes in any way, publish it out.
   */
  fun publishFeatureChange(featureValue: DbFeatureValue)


  fun publishApplicationRolloutStrategyChange(rs: DbApplicationRolloutStrategy)
}
