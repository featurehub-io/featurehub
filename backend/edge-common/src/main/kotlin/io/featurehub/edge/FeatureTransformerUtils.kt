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
    features: List<CacheEnvironmentFeature>?,
    clientAttributes: ClientContext?,
    allowExtendedProperties: Boolean
  ): List<FeatureState> {
    if (features == null) {
      return emptyList()
    }

    return try {
      features
        .map { f -> transform(f, clientAttributes, allowExtendedProperties) }
    } catch (e: Exception) {
      log.error("Failed transform", e)
      emptyList()
    }
  }

  override fun transform(
    rf: CacheEnvironmentFeature,
    clientContext: ClientContext?,
    allowExtendedProperties: Boolean
  ): FeatureState {
    val fs = FeatureState()
      .key(rf.feature.key)
      .type(rf.feature.valueType) // they are the same
      .id(rf.feature.id)
      .version(0L)
      .l(false)

    val value = rf.value
    rf.value?.let { value ->
      fs.version = value.version
      fs.l = value.locked
    }

    if (allowExtendedProperties) {
      fs.featureProperties = rf.featureProperties
    }

    clientContext?.let { sdkContext ->
      val fsStrategies = value?.rolloutStrategies?.map { rs: CacheRolloutStrategy -> toFeatureRolloutStrategy(rs) }
        ?: listOf()

      if (sdkContext.isClientEvaluation && value != null) {
        fs.strategies(fsStrategies)
        fs.value(value.value)
      } else {
        val applied = applyFeature.applyFeature(
          fsStrategies,
          rf.feature.key,
          value?.id.toString() ?: rf.feature.id.toString(),
          sdkContext
        )

        fs.value(
          if (applied.isMatched) applied.value else value?.value
        )

        // return
        fs.v(applied.strategyId)
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
