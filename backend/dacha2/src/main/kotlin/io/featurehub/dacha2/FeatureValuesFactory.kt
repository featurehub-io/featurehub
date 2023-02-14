package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.PublishEnvironment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

interface FeatureValues {
  fun getFeatures(): Collection<CacheEnvironmentFeature>
  val environment: PublishEnvironment
//  fun getEnvironment(): PublishEnvironment?
  fun getEtag(): String?
}

class EnvironmentFeatures(override val environment: PublishEnvironment) : FeatureValues {
  private val log: Logger = LoggerFactory.getLogger(EnvironmentFeatures::class.java)
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

  fun setFeatureValue(feature: CacheEnvironmentFeature) {
    val id = feature.feature.id

    val existed = features.containsKey(id)
    if (!existed) { // this is just a "just in case", the main code never creates this situation
      log.trace("Key {} didn't exist, so adding the feature", id)
      features[id] = feature
      environment.featureValues.add(feature)
    } else {
      environment.featureValues.find { it.feature.id == id }?.let {
        log.trace("replacing feature {} with {}", it.value, feature.value)
        it.value = feature.value
      }
    }

    calculateEtag()
  }

  fun setFeature(feature: CacheEnvironmentFeature) {
    val id = feature.feature.id

    val existed = features.containsKey(id)
    if (!existed) {
      features[id] = feature
      environment.featureValues.add(feature)

      // etag only uses the ID of the feature, which never changes
      calculateEtag()
    } else {
      environment.featureValues.find { it.feature.id == id }?.let {
        it.feature = feature.feature
      }
    }
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
