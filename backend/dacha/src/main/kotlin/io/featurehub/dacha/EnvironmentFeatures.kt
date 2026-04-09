package io.featurehub.dacha

import io.featurehub.dacha.model.CacheEnvironmentFeature
import io.featurehub.dacha.model.PublishEnvironment
import jakarta.validation.Valid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.ArrayList
import java.util.HexFormat
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import java.util.stream.Collectors

class FilteredEnvironmentFeatures(private val envFeatures: io.featurehub.dacha.EnvironmentFeatures, private val filters: List<UUID>): InternalCache.FeatureValues {
  val filteredFeatures = envFeatures.features.filter { feature ->
    feature.feature.filters?.firstOrNull { it in filters } != null
  }
  val calculatedEtag: String =EnvironmentFeatures.etagCalculator(filteredFeatures)

  override val features: Collection<CacheEnvironmentFeature>
    get() = filteredFeatures

  override val environment: PublishEnvironment
    get() = envFeatures.environment

  override val etag: String
    get() = calculatedEtag
}

class EnvironmentFeatures(override val environment: PublishEnvironment) : InternalCache.FeatureValues {
  // Feature::id, CacheFeatureValue
  private val _features: MutableMap<UUID, CacheEnvironmentFeature>
  private var _etag: String

  init {
    this._features = ConcurrentHashMap<UUID, CacheEnvironmentFeature>(
      environment.getFeatureValues().stream()
        .collect(
          Collectors.toMap(
            Function { f: CacheEnvironmentFeature? -> f!!.getFeature().getId() },
            Function.identity<@Valid CacheEnvironmentFeature?>()
          )
        )
    )

    _etag = etagCalculator(features)
  }

  override val features: Collection<CacheEnvironmentFeature>
    get() = _features.values

  // the UUID is the FEATURE's UUID NOT the feature value's one
  fun get(id: UUID?): CacheEnvironmentFeature? {
    return _features.get(id)
  }

  fun set(feature: CacheEnvironmentFeature) {
    _features.put(feature.getFeature().getId(), feature)

    // modify the copy so no sync issues
    val featureValues: MutableList<CacheEnvironmentFeature?> = ArrayList<CacheEnvironmentFeature?>(
      environment.getFeatureValues()
    )

    featureValues.removeIf { f: CacheEnvironmentFeature? -> f!!.getFeature().getId() == feature.getFeature().getId() }
    featureValues.add(feature)

    environment.setFeatureValues(featureValues)
    _etag = etagCalculator(features)
  }

  fun remove(id: UUID) {
    _features.remove(id)

    val featureValues = environment.featureValues.toMutableList()

    featureValues.removeIf { f -> f.feature.id == id }

    environment.featureValues = featureValues

    _etag = etagCalculator(features)
  }

  override val etag: String
    get() = _etag

  companion object {
    private val log: Logger = LoggerFactory.getLogger(io.featurehub.dacha.EnvironmentFeatures::class.java)

    fun etagCalculator(featureValues: Collection<CacheEnvironmentFeature>): String {
      // we convert to list to protect against changes while we are evaluating it
      val calcTag = featureValues.toList()
        .map { fvci ->
          fvci.feature.id.toString() + fvci.feature.version + "-" + (fvci.value?.version?.toString() ?: "0000")
        }
        .joinToString("-")

      val messageDigest = MessageDigest.getInstance("MD5")!!
      val hashBytes = messageDigest.digest(calcTag.toByteArray(StandardCharsets.UTF_8))

      val newEtag = HexFormat.of().formatHex(hashBytes)

      log.trace("etag is now {} (from '{}')", newEtag, calcTag)

      return newEtag
    }
  }
}
