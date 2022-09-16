package io.featurehub.db.publish

import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.model.*
import io.featurehub.mr.events.common.CacheBroadcast
import io.featurehub.mr.events.common.CacheSource
import java.util.*

/**
 *
 */
class DummyPublisher : CacheSource {
  override fun publishObjectsAssociatedWithCache(cacheName: String) {}
  override fun publishFeatureChange(featureValue: DbFeatureValue) {}
  override fun deleteFeatureChange(feature: DbApplicationFeature, environmentId: UUID) {}
  override fun updateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {}
  override fun deleteServiceAccount(id: UUID) {}
  override fun updateEnvironment(environment: DbEnvironment, publishAction: PublishAction) {}
  override fun deleteEnvironment(id: UUID) {}
  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction) {}
  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction, featureKey: String) {}
  override fun publishRolloutStrategyChange(rs: DbRolloutStrategy) {}
}
