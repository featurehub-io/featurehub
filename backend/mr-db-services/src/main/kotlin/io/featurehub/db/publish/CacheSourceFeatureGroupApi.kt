package io.featurehub.db.publish

import io.featurehub.mr.model.RolloutStrategy
import java.util.*

/**
 * represents an individual value for a feature and the associated strategy in an environment
 */
data class CacheSourceCollectedStrategy(
  val name: String,
  val envId: UUID,
  val featureId: UUID,
  val value: Any?,
  val featureGroupId: UUID
)

interface CacheSourceFeatureGroupApi {
  // returns a Map<Feature-ID,List-of-Rollout-Strategies> (note NOT feature-value id)
  fun collectStrategiesFromGroupsForEnvironment(envId: UUID): Map<UUID, List<RolloutStrategy>>
  fun collectStrategiesFromGroupsForEnvironmentFeature(envId: UUID, featureId: UUID): List<RolloutStrategy>
  fun collectStrategiesFromEnvironmentsWithFeatures(envId: List<UUID>, featureIds: List<UUID>): List<CacheSourceCollectedStrategy>
}
