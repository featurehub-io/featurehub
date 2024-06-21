package io.featurehub.messaging.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.featurehub.messaging.model.FeatureMessagingUpdate
import io.featurehub.messaging.model.MessagingRolloutStrategy
import io.featurehub.messaging.model.StrategyUpdateType
import io.featurehub.mr.model.FeatureValueType
import io.featurehub.utils.FallbackPropertyConfig
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

interface FeatureMessageFormatter {
  fun enhanceMessagingUpdateForHandlebars(fmData: FeatureMessagingUpdate): Map<String, Any>

  /**
   * This takes a message and formats it using Handlebars. It will SHA the message and store the compiled template using,
   * an LRU cache so it doesn't need to keep recompiling the message format over and over again
   */
  fun formatMessage(data: Map<String, Any>, fmt: String): String
}

class FeatureMessageFormatterImpl : FeatureMessageFormatter {
  companion object {
    val maxValueLength = FallbackPropertyConfig.getConfig("slack.value-max-length", "150").toInt()

    val mapper = ObjectMapper().apply {
      registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
    }

    val ref = object : TypeReference<Map<String, Any>>() {}
    private val handlebars = Handlebars()
  }

  private val templateCache: LoadingCache<String, Template> = CacheBuilder.newBuilder()
    .maximumSize(FallbackPropertyConfig.getConfig("handlebars.cache-size", "1000").toInt().toLong())
    .build(object : CacheLoader<String, Template>() {
      override fun load(key: String): Template {
        return handlebars.compileInline(key)
      }
    })

  private fun truncValue(valueType: FeatureValueType, strat: MessagingRolloutStrategy): MessagingRolloutStrategy {
    if (strat.percentage != null) {
      val percent = strat.percentage
      if (percent != null) {
        strat.percentage = percent/10000
      }
    }
    if (strat.value == null || valueType != FeatureValueType.JSON && valueType != FeatureValueType.STRING) {
      return strat
    }

    if(strat.value.toString().length > maxValueLength) {
      strat.value = strat.value.toString().take(maxValueLength) + "...(value truncated)"
    }

    strat.value = strat.value.toString().replace('\"', '\u0022')

    return strat
  }

  override fun enhanceMessagingUpdateForHandlebars(fmData: FeatureMessagingUpdate): Map<String, Any> {
    fmData.featureValueUpdated?.let { fv ->
      if (fmData.featureValueType == FeatureValueType.JSON || fmData.featureValueType == FeatureValueType.STRING) {
        if (fv.updated != null) {
          fv.updated = fv.updated.toString().take(maxValueLength).replace('\"', '\u0022')
        }
        if (fv.previous != null) {
          fv.previous = fv.previous.toString().take(maxValueLength).replace('\"', '\u0022')
        }
      }
    }
    // convert the object tree into a map
    val data = mapper.readValue(mapper.writeValueAsString(fmData), ref).toMutableMap()

    fmData.additionalInfo?.let { aInfo ->
      data.putAll(aInfo)
    }

    data["whenUpdatedReadable"] = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(fmData.whenUpdated)

    fmData.strategiesUpdated?.let { strategies ->
      if (strategies.isNotEmpty()) {
        strategies.filter { it.updateType == StrategyUpdateType.ADDED }
          .map { truncValue(fmData.featureValueType, it.newStrategy!!) }.let {
          data["addedStrategies"] = it
        }

        strategies.filter { it.updateType == StrategyUpdateType.CHANGED }.map {
          mapOf<String, Any?>(
            Pair("nameChanged", it.newStrategy?.name != it.oldStrategy?.name),
            Pair("valueChanged", it.newStrategy?.value != it.oldStrategy?.value),
                  Pair("percentageChanged", it.newStrategy?.percentage != it.oldStrategy?.percentage),
                  Pair("attributesChanged", it.newStrategy?.attributes != it.oldStrategy?.attributes),
                  Pair("newStrategy", truncValue(fmData.featureValueType, it.newStrategy!!)),
                  Pair("oldStrategy", truncValue(fmData.featureValueType, it.oldStrategy!!)),
                  )
        }.let {
          data["updatedStrategies"] = it
        }

        strategies.filter { it.updateType == StrategyUpdateType.DELETED }.map { truncValue(fmData.featureValueType, it.oldStrategy!!) }.let {
          data["deletedStrategies"] = it
        }
      }
    }

    fmData.featureValueUpdated?.let { fv ->
      if (fmData.featureValueType == FeatureValueType.JSON || fmData.featureValueType == FeatureValueType.STRING) {
        if (fv.updated != null) {
          fv.updated = fv.updated.toString().take(maxValueLength).replace('\"', '\u0022')
        }
        if (fv.previous != null) {
          fv.previous = fv.previous.toString().take(maxValueLength).replace('\"', '\u0022')
        }
      }
    }

    fmData.lockUpdated?.let { locked ->
      data["wasLocked"] = !locked.previous && locked.updated
    }
    fmData.retiredUpdated?.let { retired ->
      data["wasRetired"] = !retired.previous && retired.updated
    }

    return data
  }

  override fun formatMessage(data: Map<String, Any>, fmt: String): String {
    return templateCache.get(fmt).apply(data)
  }
}
