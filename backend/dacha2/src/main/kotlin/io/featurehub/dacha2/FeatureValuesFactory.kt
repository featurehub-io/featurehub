package io.featurehub.dacha2

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.PublishEnvironment
import io.featurehub.utils.FallbackPropertyConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet


interface FeatureValues {
  fun getFeatures(): Collection<CacheEnvironmentFeature>
  val environment: PublishEnvironment
  fun getEtag(): String
}

class FilteredEnvironmentFeatures(private val envFeatures: EnvironmentFeatures, private val filters: List<UUID>): FeatureValues {
  val filteredFeatures = envFeatures.getFeatures().filter { feature ->
    feature.feature.filters?.firstOrNull { it in filters } != null
  }
  val calculatedEtag: String = EnvironmentFeatures.etagCalculator(filteredFeatures)

  override fun getFeatures(): Collection<CacheEnvironmentFeature> {
    return filteredFeatures
  }

  override val environment: PublishEnvironment
    get() = envFeatures.environment

  override fun getEtag(): String {
    return calculatedEtag
  }
}

class EnvironmentFeatures(val env: PublishEnvironment) : FeatureValues {
  private val log: Logger = LoggerFactory.getLogger(EnvironmentFeatures::class.java)

  // Feature::id, CacheFeatureValue
  private val features: MutableMap<UUID, CacheEnvironmentFeature>
  private var etag: String
  private val featureValues =
    ConcurrentSkipListSet<CacheEnvironmentFeature> { t1, t2 -> t1.feature.id.compareTo(t2.feature.id) }

  init {
//    if (log.isTraceEnabled) {
      val uniqueUuid = env.featureValues.map { it.feature.id }.distinct()
      if (uniqueUuid.size != env.featureValues.size) {
        log.error("We have duplicates in {} - this should NEVER happen", env)
      }
//    }
    features = ConcurrentHashMap(env.featureValues.associate { f -> f.feature.id to f }.toMutableMap())
    featureValues.addAll(env.featureValues)

    etag = etagCalculator(featureValues, "<new>")
  }

  fun calculateEtag() {
    etag = etagCalculator(featureValues)
  }

  val featureCount: Int
    get() = features.size

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
      if (useValue && (existed.value?.version ?: -1) <= (feature.value?.version ?: -1)) {
        log.trace("replacing feature {} with {}", existed.value, feature.value)
        existed.value = feature.value
      } else if (useValue) {
        log.trace("skipping feature value {} with {}", existed.value, feature.value)
      }

      if (existed.feature.version <= feature.feature.version) {
        log.trace("replacing feature {} with {}", existed.feature, feature.feature)
        existed.feature = feature.feature
      } else {
        log.trace("skipping replacing feature {} with {}", existed.feature, feature.feature)
      }

      existed.featureProperties = feature.featureProperties

      log.trace("new entry in feature array is {}", existed)
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

  companion object {
    private val log: Logger = LoggerFactory.getLogger(EnvironmentFeatures::class.java)
    private val HEX_FORMAT = HexFormat.of()

    fun etagCalculator(featureValues: Collection<CacheEnvironmentFeature>, priorEtag: String): String {
      // we convert to list to protect against changes while we are evaluating it
      val calcTag = featureValues.toList()
        .map { fvci ->
          fvci.feature.id.toString() + fvci.feature.version + "-" + (fvci.value?.version?.toString() ?: "0000")
        }
        .joinToString("-")

      val messageDigest = MessageDigest.getInstance("MD5")!!
      val hashBytes = messageDigest.digest(calcTag.toByteArray(StandardCharsets.UTF_8))

      val newEtag = HEX_FORMAT.formatHex(hashBytes)

      if (logUnchangingEtags && priorEtag == newEtag) {
        log.warn("etag {} is the same for {}", newEtag, calcTag)
      }

      log.trace("etag was `{}`, is now {} (from '{}')", priorEtag, newEtag, calcTag)

      return newEtag
    }

    val logUnchangingEtags = FallbackPropertyConfig.getConfig("dacha2.log-unchanging-etags", "false") == "true"
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
