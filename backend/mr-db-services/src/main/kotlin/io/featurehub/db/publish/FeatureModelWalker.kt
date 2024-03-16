package io.featurehub.db.publish

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.featurehub.dacha.model.CacheFeature
import io.featurehub.dacha.model.CacheFeatureValue
import io.featurehub.db.model.DbApplicationFeature
import io.featurehub.db.model.DbFeatureValue
import io.featurehub.mr.model.RolloutStrategy
import io.featurehub.utils.FallbackPropertyConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.UUID

interface FeatureModelWalker {
  fun walk(
    feature: DbApplicationFeature,
    featureValue: DbFeatureValue?,
    cacheFeature: CacheFeature,
    toCacheFeatureValue: CacheFeatureValue?,
    featureGroupRolloutStrategies: List<RolloutStrategy>?
  ): Map<String, String?>?

  val isEnabled: Boolean
  val isUsingMetadata: Boolean
}

internal data class TemplateData(val feature: DbApplicationFeature,
                                 val featureValue: DbFeatureValue?,
                                 val pubFeature: CacheFeature,
                                 val pubFeatureValue: CacheFeatureValue?,
                                 val fgStrategies: List<RolloutStrategy>?,
  val metadata:  Any?
  )

internal data class TemplateFeature(val version: Long, val templateData: Any?)
/**
 * This allows system operators to yam extra data into the cached feature from anywhere in the model at the cost of
 * publishing performance.
 */
class FeatureModelWalkerService : FeatureModelWalker {
  private val walkerTemplates = mutableMapOf<String, Template>()
  private val handlebars = Handlebars()
  private val metadataUsed: Boolean
  private val objectMapper = ObjectMapper().apply {
    registerModule(KotlinModule.Builder().build())
    .registerModule(JavaTimeModule())
  }
  private val featureMetadataCache: Cache<UUID,TemplateFeature> = CacheBuilder.newBuilder()
    .maximumSize(Integer.valueOf(FallbackPropertyConfig.getConfig("sdk.feature.properties.size", "100")).toLong())
    .build()

  companion object {
    private val log: Logger = LoggerFactory.getLogger(FeatureModelWalkerService::class.java)
    private val typeRef = object: TypeReference<Any>() {}
  }

  init {
    var usingMetadata = false
    FallbackPropertyConfig.getConfig("sdk.feature.properties")?.let { cacheYam ->
      cacheYam.trim().split(",").map { s -> s.trim() }.filter { it.isNotEmpty() }.forEach { yam ->
        val parts = yam.split("=")
        if (parts.size == 2) {
          if (parts[1].startsWith("#")) {
            val filename = parts[1].substring(1)
            try {
              val text = Path.of(filename).toFile().readText()
              usingMetadata = usingMetadata || text.contains("metadata.")
              walkerTemplates.put(parts[0], handlebars.compileInline(text))
            } catch (e: Exception) {
              log.error("Unable to load filename {} for walking models", filename)
            }
          } else {
            usingMetadata = usingMetadata || parts[1].contains("metadata.")
            walkerTemplates.put(parts[0], handlebars.compileInline(parts[1]))
          }
        }
      }
    }
    metadataUsed = usingMetadata
  }

  override fun walk(
    feature: DbApplicationFeature,
    featureValue: DbFeatureValue?,
    cacheFeature: CacheFeature,
    toCacheFeatureValue: CacheFeatureValue?,
    featureGroupRolloutStrategies: List<RolloutStrategy>?
  ): Map<String, String?>? {
    if (walkerTemplates.isEmpty()) return null

    val metadata = if (metadataUsed) convertMetadata(feature) else null

    log.trace("walker: metadata for {} is {}", feature.key, metadata)

    return walkerTemplates.map { it.key to it.value.apply(
      TemplateData(feature, featureValue, cacheFeature,
        toCacheFeatureValue, featureGroupRolloutStrategies,
        metadata))?.trim() }
      .filter { (it.second ?: "").isNotEmpty() } // sparse filter, don't send empty fields
      .toMap()
  }

  override val isEnabled: Boolean
    get() = walkerTemplates.isNotEmpty()
  override val isUsingMetadata: Boolean
    get() = isEnabled && this.metadataUsed

  fun exists(fieldName: String): Boolean {
    return walkerTemplates.containsKey(fieldName)
  }

  private fun convertMetadata(feature: DbApplicationFeature): Any? {
    if (feature.metaData != null) {
      val cached = featureMetadataCache.get(feature.id) {
        try {
          val obj = objectMapper.readValue(feature.metaData, typeRef)
          TemplateFeature(feature.version, obj)
        } catch (e: Exception) {
          log.warn("Unable to parse feature {}", feature.name)
          TemplateFeature(feature.version, null)
        }
      }

      if (cached.version != feature.version) {
        // remove it from the cache and call ourselves again
        featureMetadataCache.invalidate(feature.id)
        return convertMetadata(feature)
      }

      return cached.templateData
    }

    return null
  }
}
