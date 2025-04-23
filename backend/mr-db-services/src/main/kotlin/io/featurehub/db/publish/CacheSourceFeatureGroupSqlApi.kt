package io.featurehub.db.publish

import io.featurehub.db.model.query.QDbFeatureGroup
import io.featurehub.db.model.query.QDbFeatureGroupFeature
import io.featurehub.mr.model.RolloutStrategy
import java.util.*

class CacheSourceFeatureGroupSqlApi : CacheSourceFeatureGroupApi {
  private fun collectStrategiesFromGroupsForEnvironmentFeatures(
    envId: UUID,
    featureIds: List<UUID>
  ): Map<UUID, List<RolloutStrategy>> {
    val data = mutableMapOf<UUID, MutableList<RolloutStrategy>>()

    var finder = QDbFeatureGroup()
      .select(QDbFeatureGroup.Alias.features.key.feature, QDbFeatureGroup.Alias.strategies)
      .environment.id.eq(envId)
      .whenArchived.isNull
      .strategies.isNotNull
      .features.fetch()
      .orderBy().order.asc()

    if (featureIds.isNotEmpty()) {
      finder = finder.features.key.feature.`in`(featureIds)
    }

    finder
      .findList().forEach { fg ->
        fg.strategies?.let {
          val validStrategies = it.filter { s -> s.id != null }
          if (validStrategies.isNotEmpty()) {
            val strat = validStrategies.first()

            fg.features.forEach { feat ->
              val list = data.computeIfAbsent(feat.key.feature) { _ -> mutableListOf() }
              list.add(
                RolloutStrategy().id(strat.id).name(strat.name).percentage(strat.percentage)
                  .percentageAttributes(strat.percentageAttributes)
                  .attributes(strat.attributes).value(FeatureGroupHelper.cast(feat.value, feat.feature.valueType))
              )
            }
          }
        }
      }

    return data.toMap()
  }

  override fun collectStrategiesFromGroupsForEnvironment(envId: UUID): Map<UUID, List<RolloutStrategy>> {
    return collectStrategiesFromGroupsForEnvironmentFeatures(envId, listOf())
  }

  override fun collectStrategiesFromGroupsForEnvironmentFeature(envId: UUID, featureId: UUID): List<RolloutStrategy> {
    val data = collectStrategiesFromGroupsForEnvironmentFeatures(envId, listOf(featureId))

    return data[featureId] ?: listOf()
  }

  override fun collectStrategiesFromEnvironmentsWithFeatures(
    envId: List<UUID>,
    featureIds: List<UUID>
  ): List<CacheSourceCollectedStrategy> {
    val collected = mutableListOf<CacheSourceCollectedStrategy>()

    // neither of these is a valid use case, so skip asking the db
    if (envId.isEmpty() || featureIds.isEmpty()) {
      return collected
    }

    QDbFeatureGroupFeature().key.feature.`in`(featureIds).group.environment.id.`in`(envId)
      .group.whenArchived.isNull
      .select(
        QDbFeatureGroupFeature.Alias.group.strategies, QDbFeatureGroupFeature.Alias.value,
        QDbFeatureGroupFeature.Alias.group.environment.id,
        QDbFeatureGroupFeature.Alias.key.feature,
        QDbFeatureGroupFeature.Alias.feature.valueType,
        QDbFeatureGroupFeature.Alias.key.group,
        QDbFeatureGroupFeature.Alias.group.id
      )
      .orderBy().group.order.asc()
      .findList().forEach { fgFeature ->
        fgFeature.group.strategies?.let { strat ->
          if (strat.isNotEmpty()) {
            collected.add(
              CacheSourceCollectedStrategy(
                strat[0].name,
                fgFeature.group.environment.id,
                fgFeature.key.feature,
                FeatureGroupHelper.cast(fgFeature.value, fgFeature.feature.valueType),
                fgFeature.group.id
              )
            )
          }
        }
      }

    return collected
  }
}
