package io.featurehub.edge

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.CacheRolloutStrategy
import io.featurehub.dacha.model.CacheRolloutStrategyAttribute
import io.featurehub.edge.strategies.ApplyFeature
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.FeatureRolloutStrategy
import io.featurehub.sse.model.FeatureRolloutStrategyAttribute
import io.featurehub.sse.model.FeatureState
import io.featurehub.strategies.matchers.MatcherRegistry
import io.featurehub.strategies.percentage.PercentageMumurCalculator
import org.slf4j.LoggerFactory
import java.util.stream.Collectors

class FeatureTransformerUtils : FeatureTransformer {
  private val applyFeature = ApplyFeature(PercentageMumurCalculator(), MatcherRegistry())
  override fun transform(
    features: List<CacheEnvironmentFeature>?, clientAttributes: ClientContext?
  ): List<FeatureState?>? {
    return try {
      features!!.stream()
        .map { f: CacheEnvironmentFeature? -> transform(f, clientAttributes) }
        .collect(Collectors.toList())
    } catch (e: Exception) {
      log.error("Failed transform", e)
      ArrayList()
    }
  }

  override fun transform(rf: CacheEnvironmentFeature?, clientAttributes: ClientContext?): FeatureState? {
    val fs = FeatureState()
      .key(rf!!.feature.key)
      .type(rf.feature.valueType) // they are the same
      .id(rf.feature.id)
    if (rf.value == null) {
      fs.version = 0L
      fs.l = false
    } else {
      fs.version = rf.value!!.version
      fs.l = rf.value!!.locked
      if (clientAttributes != null) {
        val fsStrategies = if (rf.value!!
            .rolloutStrategies != null
        ) rf.value!!.rolloutStrategies!!.stream()
          .map { rs: CacheRolloutStrategy -> toFeatureRolloutStrategy(rs) }
          .collect(Collectors.toList()) else ArrayList()
        if (clientAttributes.isClientEvaluation && rf.value != null) {
          fs.strategies(fsStrategies)
          fs.value(rf.value!!.value)
        } else {
          val applied = applyFeature.applyFeature(
            fsStrategies,
            rf.feature.key,
            rf.value!!.id.toString(),
            clientAttributes
          )
          fs.value(
            if (applied.isMatched) applied.value else if (rf.value == null) null else rf.value!!
              .value
          )
          // return
          fs.v(applied.strategyId)
        }
      }
    }
    return fs
  }

  private fun toFeatureRolloutStrategy(rs: CacheRolloutStrategy): FeatureRolloutStrategy {
    return FeatureRolloutStrategy()
      .id(rs.id)
      .attributes(
        rs.attributes.stream()
          .map { rsa: CacheRolloutStrategyAttribute -> toFeatureRolloutStrategyAttribute(rsa) }
          .collect(Collectors.toList()))
      .percentage(rs.percentage)
      .percentageAttributes(rs.percentageAttributes)
      .value(rs.value)
  }

  private fun toFeatureRolloutStrategyAttribute(
    rsa: CacheRolloutStrategyAttribute
  ): FeatureRolloutStrategyAttribute {
    return FeatureRolloutStrategyAttribute()
      .conditional(rsa.conditional)
      .type(rsa.type)
      .fieldName(rsa.fieldName)
      .values(rsa.values)
  }

  companion object {
    private val log = LoggerFactory.getLogger(FeatureTransformerUtils::class.java)
  }
}
