package io.featurehub.db.publish

import io.featurehub.mr.model.RolloutStrategy
import java.util.*

interface CacheSourceFeatureGroupApi {
  // returns a Map<Feature-ID,List-of-Rollout-Strategies> (note NOT feature-value id)
  fun collectStrategiesFromGroupsForEnvironment(envId: UUID): Map<UUID, List<RolloutStrategy>>
  fun collectStrategiesFromGroupsForEnvironmentFeature(envId: UUID, featureId: UUID): List<RolloutStrategy>
}
