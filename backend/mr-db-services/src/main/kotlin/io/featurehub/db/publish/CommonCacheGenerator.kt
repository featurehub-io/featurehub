package io.featurehub.db.publish

import io.featurehub.dacha.model.CacheRolloutStrategy
import io.featurehub.dacha.model.CacheRolloutStrategyAttribute
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.db.model.DbPortfolioStrategyForFeatureValue
import io.featurehub.db.model.DbStrategyForFeatureValue
import io.featurehub.db.model.query.QDbApplicationRolloutStrategy
import io.featurehub.db.model.query.QDbPortfolioRolloutStrategy
import io.featurehub.db.model.query.QDbPortfolioStrategyForFeatureValue
import io.featurehub.db.model.query.QDbStrategyForFeatureValue
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.mr.model.RolloutStrategyAttribute
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is used by MR and Edge-Rest when data is required. The way that data types are transformed is the same
 * just the way they are extracted are different.
 */
interface CommonCacheGenerator {
  // combines the custom and shared rollout strategies

  fun collectCombinedRolloutStrategies(
    featureValue: DbFeatureValue,
    featureGroupRolloutStrategies: List<RolloutStrategy>?
  ): List<CacheRolloutStrategy>
}

class DefaultCommonCacheGenerator : CommonCacheGenerator {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(DefaultCommonCacheGenerator::class.java)
  }

  override fun collectCombinedRolloutStrategies(
    featureValue: DbFeatureValue,
    featureGroupRolloutStrategies: List<RolloutStrategy>?
  ): List<CacheRolloutStrategy> {
    log.trace("cache combine strategies")

    val allStrategies = mutableListOf<CacheRolloutStrategy>()
    allStrategies.addAll(featureValue.rolloutStrategies.map { rs -> fromRolloutStrategy(rs) })

    val activeSharedStrategies = QDbStrategyForFeatureValue()
      .select(QDbStrategyForFeatureValue.Alias.value)
      .featureValue.id.eq(featureValue.id)
      .enabled.isTrue
      .rolloutStrategy.fetch(QDbApplicationRolloutStrategy.Alias.strategy, QDbApplicationRolloutStrategy.Alias.shortUniqueCode)
      .findList()

    allStrategies.addAll(activeSharedStrategies.filter { !it.rolloutStrategy.strategy.disabled }.map { shared ->
      val rs = fromApplicationRolloutStrategy(shared)
      rs.value = if (featureValue.feature.valueType == FeatureValueType.BOOLEAN) "true" == shared.value else shared.value // the value associated with the shared strategy is set here not in the strategy itself
      rs
    })

    val activePortfolioStrategies = QDbPortfolioStrategyForFeatureValue()
      .select(QDbPortfolioStrategyForFeatureValue.Alias.value)
      .featureValue.id.eq(featureValue.id)
      .enabled.isTrue
      .rolloutStrategy.fetch(QDbPortfolioRolloutStrategy.Alias.strategy, QDbPortfolioRolloutStrategy.Alias.shortUniqueCode)
      .findList()

    allStrategies.addAll(activePortfolioStrategies.filter { !it.rolloutStrategy.strategy.disabled }.map { shared ->
      val rs = fromPortfolioRolloutStrategy(shared)
      rs.value = if (featureValue.feature.valueType == FeatureValueType.BOOLEAN) "true" == shared.value else shared.value // the value associated with the shared strategy is set here not in the strategy itself
      rs
    })

    featureGroupRolloutStrategies?.let { fgStrategies ->
      allStrategies.addAll(fgStrategies.map { fromRolloutStrategy(it) })
    }

    return allStrategies
  }


  private fun fromRolloutStrategyAttribute(rsa: RolloutStrategyAttribute): CacheRolloutStrategyAttribute {
    return CacheRolloutStrategyAttribute()
      .conditional(rsa.conditional)
      .values(rsa.values)
      .fieldName(rsa.fieldName)
      .type(rsa.type)
  }



  private fun fromRolloutStrategy(rs: RolloutStrategy): CacheRolloutStrategy {
    return CacheRolloutStrategy()
      .id(rs.id ?: "rs-id")
      .percentage(rs.percentage)
      .percentageAttributes(rs.percentageAttributes)
      .value(rs.value)
      .attributes(if (rs.attributes == null) mutableListOf() else rs.attributes!!
        .map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
      )
  }

  private fun fromApplicationRolloutStrategy(rs: DbStrategyForFeatureValue): CacheRolloutStrategy {
    val value = if (rs.featureValue.feature.valueType == FeatureValueType.BOOLEAN) "true".equals(rs.value) else rs.value
    return CacheRolloutStrategy()
      .id(rs.rolloutStrategy.shortUniqueCode)
      .percentage(rs.percentageOverride ?: rs.rolloutStrategy.strategy.percentage)
      .percentageAttributes(rs.rolloutStrategy.strategy.percentageAttributes)
      .value(value)
      .attributes(if (rs.rolloutStrategy.strategy.attributes == null) mutableListOf() else rs.rolloutStrategy.strategy.attributes!!
        .map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
      )
  }

  private fun fromPortfolioRolloutStrategy(rs: DbPortfolioStrategyForFeatureValue): CacheRolloutStrategy {
    val value = if (rs.featureValue.feature.valueType == FeatureValueType.BOOLEAN) "true".equals(rs.value) else rs.value
    return CacheRolloutStrategy()
      .id(rs.rolloutStrategy.shortUniqueCode)
      .percentage(rs.percentageOverride ?: rs.rolloutStrategy.strategy.percentage)
      .percentageAttributes(rs.rolloutStrategy.strategy.percentageAttributes)
      .value(value)
      .attributes(if (rs.rolloutStrategy.strategy.attributes == null) mutableListOf() else rs.rolloutStrategy.strategy.attributes!!
        .map { rsa: RolloutStrategyAttribute -> fromRolloutStrategyAttribute(rsa) }
      )
  }
}
