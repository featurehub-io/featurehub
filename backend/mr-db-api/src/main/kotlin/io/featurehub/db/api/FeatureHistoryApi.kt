package io.featurehub.db.api

import io.featurehub.mr.model.FeatureHistoryList
import java.util.*

interface FeatureHistoryApi {
  fun listHistory(appId: UUID, environmentIds: List<UUID>, versions: List<Long>, keys: List<String>, featureIds: List<UUID>, max: Int?, startAt: Int?): FeatureHistoryList
}
