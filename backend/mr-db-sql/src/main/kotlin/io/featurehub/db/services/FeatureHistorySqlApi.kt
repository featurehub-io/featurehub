package io.featurehub.db.services

import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.query.QDbFeatureValueVersion
import io.featurehub.mr.model.AnemicPerson
import io.featurehub.mr.model.FeatureHistoryItem
import io.featurehub.mr.model.FeatureHistoryList
import io.featurehub.mr.model.FeatureHistoryValue
import java.time.ZoneOffset
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
      .select(
        QDbFeatureValueVersion.Alias.id,
        QDbFeatureValueVersion.Alias.defaultValue,
        QDbFeatureValueVersion.Alias.versionFrom,
        QDbFeatureValueVersion.Alias.locked,
        QDbFeatureValueVersion.Alias.retired,
        QDbFeatureValueVersion.Alias.rolloutStrategies,
        QDbFeatureValueVersion.Alias.whenCreated,
        QDbFeatureValueVersion.Alias.whoUpdated.id,
        QDbFeatureValueVersion.Alias.whoUpdated.name,
        QDbFeatureValueVersion.Alias.feature.id,
        QDbFeatureValueVersion.Alias.featureValue.id,
        QDbFeatureValueVersion.Alias.featureValue.environment.id
      )
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

    val items = mutableMapOf<FeatureHistoryItem, FeatureHistoryItem>()

    data.forEach { it ->
      val key = FeatureHistoryItem().featureId(it.feature.id).featureValueId(it.id.id).envId(it.featureValue.environment.id)
      var item = items[key]

      if (item == null) {
        item = FeatureHistoryItem().featureId(it.feature.id).featureValueId(it.id.id).envId(it.featureValue.environment.id)
        items[key] = item
      }

      item!!.addHistoryItem(
        FeatureHistoryValue()
          .versionFrom(it.versionFrom)
          .value(it.defaultValue).version(it.id.version).retired(it.isRetired)
          .locked(it.isLocked).rolloutStrategies(it.rolloutStrategies).`when`(it.whenCreated.atOffset(ZoneOffset.UTC))
          .who(AnemicPerson().id(it.whoUpdated.id).name(it.whoUpdated.name))
      )
    }

    return FeatureHistoryList()
      .max(count.get().toLong())
      .items(items.values.toList())
  }
}
