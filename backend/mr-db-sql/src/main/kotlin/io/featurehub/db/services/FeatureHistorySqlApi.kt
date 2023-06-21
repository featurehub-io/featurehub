package io.featurehub.db.services

import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.query.QDbFeatureValueVersion
import io.featurehub.mr.model.FeatureHistoryList
import java.util.UUID


interface InternalFeatureHistoryApi {
}

interface FeatureHistoryApi {
  fun listHistory(appId: UUID, environmentIds: List<UUID>, versions: List<Long>, keys: List<String>, featureIds: List<UUID>, max: Int?, startAt: Int?): FeatureHistoryList
}


class FeatureHistorySqlApi : InternalFeatureHistoryApi, FeatureHistoryApi {
  fun history(environmentId: UUID, applicationFeature: UUID, featureValue: UUID): List<DbFeatureValueVersion> {
    return QDbFeatureValueVersion().feature.id.eq(applicationFeature).id.id.eq(featureValue).findList();
  }

  override fun listHistory(
    appId: UUID,
    environmentIds: List<UUID>,
    versions: List<Long>,
    keys: List<String>,
    featureIds: List<UUID>,
    max: Int?,
    startAt: Int?
  ): FeatureHistoryList {
    val highest = (max?.coerceAtLeast(1) ?: 1).coerceAtMost(100)
    val start = (startAt?.coerceAtLeast(0) ?: 0)
    var finder = QDbFeatureValueVersion()
      .feature.parentApplication.id.eq(appId)

    if (environmentIds.isNotEmpty()) {
      finder = finder.featureValue.environment.id.`in`(environmentIds)
    }

    if (featureIds.isNotEmpty()) {
      finder = finder.feature.id.`in`(featureIds)
    } else if (keys.isNotEmpty()) {
      finder = finder.feature.key.`in`(keys)
    }

    if (versions.isNotEmpty() && ((featureIds.isNotEmpty() && featureIds.size == 1) || (keys.isNotEmpty() && keys.size == 1))) {
      finder = finder.id.version.`in`(versions)
    }

    val count = finder.findFutureCount()
    val data =  finder.setMaxRows(highest).setFirstRow(start).findList()


  }
}
