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
import io.featurehub.messaging.model.StrategyUpdateType
import io.featurehub.utils.FallbackPropertyConfig
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter

interface FeatureMessageFormatter {
  fun enhanceMessagingUpdateForHandlebars(fmData: FeatureMessagingUpdate): Map<String,Any>

  /**
   * This takes a message and formats it using Handlebars. It will SHA the message and store the compiled template using,
   * an LRU cache so it doesn't need to keep recompiling the message format over and over again
   */
  fun formatMessage(data: Map<String,Any>, fmt: String): String
}

class FeatureMessageFormatterImpl : FeatureMessageFormatter {
  companion object {
    val mapper = ObjectMapper().apply {
      registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
    }

    val ref = object: TypeReference<Map<String,Any>>() {}
    private val handlebars = Handlebars()
  }

  private val templateCache: LoadingCache<String, Template> = CacheBuilder.newBuilder()
    .maximumSize(FallbackPropertyConfig.getConfig("handlebars.cache-size", "1000").toInt().toLong())
    .build(object : CacheLoader<String, Template>() {
      override fun load(key: String): Template {
        return handlebars.compileInline(key)
      }
    })

  override fun enhanceMessagingUpdateForHandlebars(fmData: FeatureMessagingUpdate): Map<String,Any> {
    // convert the object tree into a map
    val data = mapper.readValue(mapper.writeValueAsString(fmData), ref).toMutableMap()

    fmData.additionalInfo?.let { aInfo ->
      data.putAll(aInfo)
    }

    data["whenUpdatedReadable"] = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(fmData.whenUpdated)

    fmData.strategiesUpdated?.let { strategies ->
      if (strategies.isNotEmpty()) {
        strategies.filter { it.updateType == StrategyUpdateType.ADDED }.map { it.newStrategy }.let {
          data["addedStrategies"] = it
        }

        strategies.filter { it.updateType == StrategyUpdateType.CHANGED }.map {
          mapOf<String,Any?>(Pair("newStrategy", it.newStrategy), Pair("oldStrategy", it.oldStrategy), Pair("nameChanged", it.newStrategy?.name != it.oldStrategy?.name))
        }.let {
          data["updatedStrategies"] = it
        }

        strategies.filter { it.updateType == StrategyUpdateType.CHANGED }.map {
          mapOf<String,Any?>(Pair("newStrategy", it.newStrategy), Pair("oldStrategy", it.oldStrategy), Pair("valueChanged", it.newStrategy?.value != it.oldStrategy?.value))
        }.let {
          data["updatedStrategiesValues"] = it
        }

        strategies.filter { it.updateType == StrategyUpdateType.DELETED }.map { it.oldStrategy }.let {
          data["deletedStrategies"] = it
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
