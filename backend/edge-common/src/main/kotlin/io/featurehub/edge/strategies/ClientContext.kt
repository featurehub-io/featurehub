package io.featurehub.edge.strategies

import io.featurehub.edge.KeyParts
import io.featurehub.edge.model.EdgeAttributeCountryName
import io.featurehub.edge.model.EdgeAttributeDeviceName
import io.featurehub.edge.model.EdgeAttributePlatformName
import io.featurehub.edge.model.EdgeAttributeWellKnownNames
import io.featurehub.mr.model.StrategyAttributeCountryName
import io.featurehub.mr.model.StrategyAttributeDeviceName
import io.featurehub.mr.model.StrategyAttributePlatformName
import io.featurehub.mr.model.StrategyAttributeWellKnownNames
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * The client attributes are expected to be in the format key=val where val is url-encoded. It can
 * have multiple values separated by commas and there can be multiple headers.
 */
class ClientContext(val isClientEvaluation: Boolean) {
  var attributes: MutableMap<String, List<String>> = HashMap()
  fun defaultPercentageKey(): String? {
    var uKey = attributes[SESSIONKEY]
    if (uKey == null) {
      uKey = attributes[USERKEY]
    }
    return if (uKey.isNullOrEmpty()) null else uKey[0]
  }

  operator fun get(key: String, defaultValue: String): String {
    val value = attributes[key]
    return if (value.isNullOrEmpty()) {
      defaultValue
    } else value[0]
  }

  fun makeEtag(): String {
    return Integer.toHexString(attributes.hashCode())
  }

  companion object {
    const val USERKEY = "userkey"
    const val SESSIONKEY = "sessionkey"

    /**
     * Here we need to transform the incoming structure to the existing API which is used everywhere else
     */
    fun decode(headers: Map<String, List<String>>?, apiKeys: List<KeyParts>): ClientContext {
      val strategy = ClientContext(apiKeys.stream().anyMatch { k: KeyParts -> k.serviceKey.contains("*") })
      if (headers != null) {
        for (header in headers.entries) {
          // in case these leak in
          if (header.key == "apiKey") continue

          val attr = StrategyAttributeWellKnownNames.fromValue(EdgeAttributeWellKnownNames.nameMappings[header.key])?.value ?: header.key
          val initialValues = header.value.map { URLDecoder.decode(it, StandardCharsets.UTF_8) }
          val values = when (header.key) {
            "d" -> decodeDevices(initialValues)
            "c" -> decodeCountries(initialValues)
            "p" -> decodePlatforms(initialValues)
            else -> initialValues
          }

          strategy.attributes[attr] = values
        }
      }

      return strategy
    }

    private fun decodeDevices(vals: List<String>): List<String> {
      return vals.map {
        StrategyAttributeDeviceName.fromValue(EdgeAttributeDeviceName.nameMappings[it])?.value ?: StrategyAttributeDeviceName.DESKTOP.value
      }
    }


    private fun decodeCountries(vals: List<String>): List<String> {
      return vals.map {
        StrategyAttributeCountryName.fromValue(EdgeAttributeCountryName.nameMappings[it])?.value ?: StrategyAttributeCountryName.ZIMBABWE.value
      }
    }

    private fun decodePlatforms(vals: List<String>): List<String> {
      return vals.map {
        StrategyAttributePlatformName.fromValue(EdgeAttributePlatformName.nameMappings[it])?.value ?: StrategyAttributePlatformName.WINDOWS.value
      }
    }

    fun decode(headers: List<String>?, apiKeys: List<KeyParts>): ClientContext {
      val strategy = ClientContext(apiKeys.stream().anyMatch { k: KeyParts -> k.serviceKey.contains("*") })
      if (headers != null) {
        for (header in headers) {
          for (part in header.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val pos = part.indexOf("=")
            if (pos != -1) {
              val key = URLDecoder.decode(part.substring(0, pos), StandardCharsets.UTF_8)
              val value = URLDecoder.decode(part.substring(pos + 1), StandardCharsets.UTF_8)
              val vals = if (value.contains(",")) {
                Arrays.asList(*value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
              } else {
                listOf(value)
              }
              strategy.attributes[key] = vals

              // max 30 attributes
              if (strategy.attributes.size >= 30) {
                return strategy
              }
            }
          }
        }
      }
      return strategy
    }
  }
}
