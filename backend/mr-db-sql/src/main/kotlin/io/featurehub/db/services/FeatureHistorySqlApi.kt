package io.featurehub.db.services

import io.featurehub.db.api.FeatureHistoryApi
import io.featurehub.db.model.DbFeatureValueVersion
import io.featurehub.db.model.query.QDbApplicationFeature
import io.featurehub.db.model.query.QDbApplicationRolloutStrategy
import io.featurehub.db.model.query.QDbFeatureValue
import io.featurehub.db.model.query.QDbFeatureValueVersion
import io.featurehub.db.model.query.QDbPerson
import io.featurehub.mr.model.*
import java.math.BigDecimal
import java.time.ZoneOffset
import java.util.UUID


interface InternalFeatureHistoryApi

class FeatureHistorySqlApi : InternalFeatureHistoryApi, FeatureHistoryApi {
  fun history(environmentId: UUID, applicationFeature: UUID, featureValue: UUID): List<DbFeatureValueVersion> {
    return QDbFeatureValueVersion().feature.id.eq(applicationFeature).id.id.eq(featureValue).findList()
  }

  override fun listHistory(
    appId: UUID,
    environmentIds: List<UUID>,
    versions: List<Long>,
    keys: List<String>,
    featureIds: List<UUID>,
    max: Int?,
    startAt: Int?,
    orderDescending: Boolean
  ): FeatureHistoryList {
    val highest = ((max ?: 20).coerceAtLeast(1)).coerceAtMost(100)
    val start = (startAt?.coerceAtLeast(0) ?: 0)
    var finder = QDbFeatureValueVersion()
      .select(
        QDbFeatureValueVersion.Alias.id,
        QDbFeatureValueVersion.Alias.defaultValue,
        QDbFeatureValueVersion.Alias.versionFrom,
        QDbFeatureValueVersion.Alias.locked,
        QDbFeatureValueVersion.Alias.retired,
        QDbFeatureValueVersion.Alias.rolloutStrategies,
        QDbFeatureValueVersion.Alias.sharedRolloutStrategies,
        QDbFeatureValueVersion.Alias.whenCreated,
      )
      .whoUpdated.fetch(QDbPerson.Alias.id, QDbPerson.Alias.name, QDbPerson.Alias.personType, QDbPerson.Alias.email)
      .featureValue.fetch(QDbFeatureValue.Alias.id, QDbFeatureValue.Alias.environment.id)
      .feature.fetch(QDbApplicationFeature.Alias.key, QDbApplicationFeature.Alias.id, QDbApplicationFeature.Alias.valueType)
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
    finder =  finder.setMaxRows(highest).setFirstRow(start)

    if (orderDescending) {
      finder = finder.orderBy().id.version.desc()
    } else {
      finder = finder.orderBy().id.version.asc()
    }

    val data = finder.findList()

    val items = mutableMapOf<FeatureHistoryItem, FeatureHistoryItem>()

    data.forEach { it ->
      val key = FeatureHistoryItem().featureId(it.feature.id).featureValueId(it.id.id).envId(it.featureValue.environment.id)
      var item = items[key]

      if (item == null) {
        item = key.copy()
        items[key] = item
      }


       if (it.sharedRolloutStrategies.isNotEmpty()) {
        QDbApplicationRolloutStrategy().id.`in`(it.sharedRolloutStrategies.map { s -> s.strategyId }).application.id.eq(appId).findList().forEach { shared ->
          val rs = RolloutStrategy()
            .id(shared.shortUniqueCode)
            .value(convert(it.sharedRolloutStrategies.first { srs -> srs.strategyId == shared.id }.value, it.feature.valueType))
            .attributes(mutableListOf())
            .name(shared.name)

          if (shared.whenArchived != null) {
            rs.name(shared.name.split(Conversions.archivePrefix)[0])
          }

          it.rolloutStrategies.add(rs)
        }
      }

      item!!.addHistoryItem(
        FeatureHistoryValue()
          .versionFrom(it.versionFrom)
          .value(convert(it.defaultValue, it.feature.valueType)).version(it.id.version).retired(it.isRetired)
          .locked(it.isLocked).rolloutStrategies(it.rolloutStrategies).`when`(it.whenCreated.atOffset(ZoneOffset.UTC))
          .who(AnemicPerson().id(it.whoUpdated.id).name(it.whoUpdated.name).type(it.whoUpdated.personType).email(it.whoUpdated.email))
      )
    }

    return FeatureHistoryList()
      .max(count.get().toLong())
      .items(items.values.toList())
  }

  private fun convert(defaultValue: Any?, valueType: FeatureValueType): Any? {
    return when(valueType) {
      FeatureValueType.BOOLEAN -> defaultValue?.toString() == "true"
      FeatureValueType.STRING -> defaultValue as String?
      FeatureValueType.NUMBER -> defaultValue?.let { BigDecimal(it.toString()) }
      FeatureValueType.JSON -> defaultValue as String?
    }
  }

  private fun convert(defaultValue: String?, valueType: FeatureValueType): Any? {
    return when(valueType) {
      FeatureValueType.BOOLEAN -> defaultValue == "true"
      FeatureValueType.STRING -> defaultValue
      FeatureValueType.NUMBER -> defaultValue?.let { BigDecimal(it) }
      FeatureValueType.JSON -> defaultValue
    }
  }
}
