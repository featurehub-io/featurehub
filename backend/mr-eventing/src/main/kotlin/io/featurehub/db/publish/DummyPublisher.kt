package io.featurehub.db.publish

import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.db.model.*
import io.featurehub.mr.events.common.CacheSource
import java.util.*

/**
 *
 */
class DummyPublisher : CacheSource, CacheRefresherApi {
  override fun publishObjectsAssociatedWithCache() {}
  override fun publishFeatureChange(featureValue: DbFeatureValue) {}
  override fun publishApplicationRolloutStrategyChange(update: PublishAction, rs: DbApplicationRolloutStrategy) {
  }

  override fun deleteFeatureChange(feature: DbApplicationFeature, environmentId: UUID) {}
  override fun updateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {}
  override fun deleteServiceAccount(id: UUID) {}
  override fun updateEnvironment(environment: DbEnvironment, publishAction: PublishAction) {}
  override fun deleteEnvironment(id: UUID) {}
  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction) {}
  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction, featureKey: String) {}
  override fun publishApplicationRolloutStrategyChange(rs: DbApplicationRolloutStrategy) {}
  override fun refreshPortfolios(portfolioIds: List<UUID>) {
  }

  override fun refreshApplications(applicationIds: List<UUID>) {
  }

  override fun refreshEntireDatabase() {
  }
}
