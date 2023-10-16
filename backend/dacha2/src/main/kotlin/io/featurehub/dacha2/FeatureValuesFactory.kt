package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.PublishEnvironment
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

interface FeatureValues {
  fun getFeatures(): Collection<CacheEnvironmentFeature>
  val environment: PublishEnvironment
  fun getEtag(): String
}

class EnvironmentFeatures(private val env: PublishEnvironment) : FeatureValues {
  private val log: Logger = LoggerFactory.getLogger(EnvironmentFeatures::class.java)
  // Feature::id, CacheFeatureValue
  private val features: MutableMap<UUID, CacheEnvironmentFeature>
  private var etag: String
  private val featureValues = ConcurrentSkipListSet<CacheEnvironmentFeature> { t1, t2 -> t1.feature.id.compareTo(t2.feature.id) }

  init {
    features =  ConcurrentHashMap(env.featureValues.associate { f -> f.feature.id to f }.toMutableMap())
    featureValues.addAll(env.featureValues)

    etag = etagCalculator()
  }

  fun calculateEtag() {
    etag = etagCalculator()
  }


  fun etagCalculator(): String {
    // we convert to list to protect against changes while we are evaluating it
    val calcTag = featureValues.toList()
      .map { fvci -> fvci.feature.id.toString() + "-" + (fvci.value?.version ?: "0000") }
      .joinToString("-")

    log.trace("etag is {}", calcTag)

    return Integer.toHexString(calcTag.hashCode())
  }

  // the UUID is the FEATURE's UUID NOT the feature value's one
  operator fun get(id: UUID): CacheEnvironmentFeature? {
    return features[id]
  }

  private fun updateEnvironmentFeature(feature: CacheEnvironmentFeature, useValue: Boolean) {
    val id = feature.feature.id

    val existed = features[id]
    if (existed == null) { // this is just a "just in case", the main code never creates this situation
      log.trace("Key {} didn't exist, so adding the feature", id)
      features[id] = feature
      try {
        featureValues.add(feature)
      } catch (e: Exception) {
        log.warn("another version of the feature {} just got added to the set", feature)
      }
    } else {
      if (useValue) {
        log.trace("replacing feature {} with {}", existed.value, feature.value)
        existed.value = feature.value
      }

      log.trace("replacing feature {} with {}", existed.feature, feature.feature)
      existed.feature = feature.feature
    }

    env.featureValues = featureValues.toList()
    calculateEtag()
  }

  fun setFeatureValue(feature: CacheEnvironmentFeature) {
    updateEnvironmentFeature(feature, true)
  }

  fun setFeature(feature: CacheEnvironmentFeature) {
    updateEnvironmentFeature(feature, false)
  }

  fun remove(id: UUID) {
    features.remove(id)?.let {
      if (!featureValues.remove(it)) {
        // fallback to the slower remove as the value may have changed
        environment.featureValues.removeIf { it.feature.id == id }
      }

      env.featureValues = featureValues.toList()

      calculateEtag()
    }
  }

  override fun getFeatures(): Collection<CacheEnvironmentFeature> {
    return features.values
  }

  override val environment: PublishEnvironment
    get() = env

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
