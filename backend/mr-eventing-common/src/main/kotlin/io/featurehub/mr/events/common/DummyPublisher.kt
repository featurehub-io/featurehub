package io.featurehub.mr.events.common

import io.featurehub.dacha.model.PublishAction
import io.featurehub.db.api.CacheRefresherApi
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbEnvironment
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbServiceAccount
import java.util.UUID

/**
 * for situations where publishing to the cache isn't required
 */
class DummyPublisher : CacheSource, CacheRefresherApi {
  override fun publishObjectsAssociatedWithCache() {}
  override fun publishFeatureChange(featureValue: DbFeatureValue) {}

  override fun deleteFeatureChange(feature: DbApplicationFeature, environmentId: UUID) {}
  override fun updateServiceAccount(serviceAccount: DbServiceAccount, publishAction: PublishAction) {}
  override fun deleteServiceAccount(id: UUID) {}
  override fun updateEnvironment(environment: DbEnvironment, publishAction: PublishAction) {}
  override fun deleteEnvironment(id: UUID) {}
  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction) {}
  override fun publishFeatureChange(appFeature: DbApplicationFeature, update: PublishAction, featureKey: String) {}
  override fun refreshPortfolios(portfolioIds: List<UUID>) {
  }

  override fun refreshApplications(applicationIds: List<UUID>) {
  }

  override fun refreshEntireDatabase() {
  }
}
