package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.PublishEnvironment
import java.util.*

interface FeatureValues {
  fun getFeatures(): Collection<CacheEnvironmentFeature>
  val environment: PublishEnvironment
//  fun getEnvironment(): PublishEnvironment?
  fun getEtag(): String?
}

class EnvironmentFeatures(override val environment: PublishEnvironment) : FeatureValues {
  // Feature::id, CacheFeatureValue
  private val features: MutableMap<UUID, CacheEnvironmentFeature>
  private var etag: String

  init {
    features = environment.featureValues.associate { f -> f.feature.id to f }.toMutableMap()

    etag = etagCalculator()
  }

  fun calculateEtag() {
    etag = etagCalculator()
  }


  fun etagCalculator(): String {
    val calcTag = features.values
      .map { fvci -> fvci.feature.id.toString() + "-" + (fvci.value?.version ?: "0000") }
      .joinToString("-")

    return Integer.toHexString(calcTag.hashCode())
  }

  // the UUID is the FEATURE's UUID NOT the feature value's one
  operator fun get(id: UUID): CacheEnvironmentFeature? {
    return features[id]
  }

  fun set(feature: CacheEnvironmentFeature) {
    val id = feature.feature.id
    features[id] = feature

    val index = features.values.indexOfFirst { it.feature.id == id }

    if (index == -1) {
      environment.featureValues.add(feature)
    } else {
      environment.featureValues.set(index, feature)
    }

    calculateEtag()
  }

  fun remove(id: UUID) {
    features.remove(id)?.let {
      environment.featureValues.removeIf { it.feature.id == id }
      calculateEtag()
    }
  }

  override fun getFeatures(): Collection<CacheEnvironmentFeature> {
    return features.values
  }

  override fun getEtag(): String {
    return etag
  }

  override fun toString(): String {
    val collect = features.values.map { obj: CacheEnvironmentFeature -> obj.toString() }.toList()

    return String.format(
      "etag: %s, features %s", etag.ifEmpty { "000" }, collect.joinToString(", ")
    )
  }
}

interface FeatureValuesFactory {
  fun create(env: PublishEnvironment): EnvironmentFeatures
}

class FeatureValuesFactoryImpl : FeatureValuesFactory {
  override fun create(env: PublishEnvironment): EnvironmentFeatures {
    return EnvironmentFeatures(env)
  }

}
