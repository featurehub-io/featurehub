package io.featurehub.edge.ofrep

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.FeatureState
import io.featurehub.mr.model.FeatureValueType

/**
 * OpenFeature error reasons as defined by OFREP 0.4.0 (`evaluationSuccess.reason`).
 */
object OfrepReasons {
  const val STATIC = "STATIC"
  const val TARGETING_MATCH = "TARGETING_MATCH"
  const val SPLIT = "SPLIT"
  const val DISABLED = "DISABLED"
  const val UNKNOWN = "UNKNOWN"
}

/**
 * OpenFeature error codes as defined by OFREP 0.4.0.
 */
object OfrepErrorCodes {
  const val PARSE_ERROR = "PARSE_ERROR"
  const val TARGETING_KEY_MISSING = "TARGETING_KEY_MISSING"
  const val INVALID_CONTEXT = "INVALID_CONTEXT"
  const val GENERAL = "GENERAL"
  const val FLAG_NOT_FOUND = "FLAG_NOT_FOUND"
}

class OfrepInvalidContextException(message: String) : Exception(message)
class OfrepFlagNotFoundException(val key: String) :
  Exception("Flag '$key' was not found in this environment")

/**
 * Maps between OpenFeature/OFREP concepts and FeatureHub edge concepts.
 */
object OfrepMapper {
  // FeatureHub reserved attribute keys (lowercase). See ClientContext.
  const val USERKEY = "userkey"
  const val SESSIONKEY = "sessionkey"

  // Match the limit enforced by ClientContext.decode for the x-featurehub header.
  const val MAX_ATTRIBUTES = 30

  /**
   * Converts an OFREP JSON evaluation context into the edge's
   * [ClientContext]. `targetingKey` becomes FeatureHub's `userkey`
   * (the default percentage key); `session` becomes `sessionkey` when
   * supplied. Every other attribute is stringified, since FeatureHub
   * strategy matchers compare string values.
   *
   * @throws OfrepInvalidContextException when an attribute value cannot be
   *   represented (nested objects, arrays of objects).
   */
  @JvmStatic
  fun toClientContext(context: Map<String, Any?>?): ClientContext {
    val clientContext = ClientContext(false)
    if (context == null) return clientContext

    // `targetingKey` wins over an explicit `userkey` when both are present.
    context.entries.firstOrNull { it.key.equals("targetingkey", ignoreCase = true) && it.value != null }
      ?.let {
        clientContext.attributes[USERKEY] = stringify(it.key, it.value)
      }

    for ((name, value) in context) {
      if (value == null) continue
      if (clientContext.attributes.size >= MAX_ATTRIBUTES) break

      when (name.lowercase()) {
        "targetingkey" -> {} // already applied above
        USERKEY -> if (!clientContext.attributes.containsKey(USERKEY)) {
          clientContext.attributes[USERKEY] = stringify(name, value)
        }
        // FeatureHub's server-side percentage key is `sessionkey`; accept
        // both spellings so an OpenFeature `session` attribute works.
        "session", SESSIONKEY -> clientContext.attributes[SESSIONKEY] = stringify(name, value)
        else -> clientContext.attributes[name] = stringify(name, value)
      }
    }

    return clientContext
  }

  private fun stringify(name: String, value: Any?): List<String> = when (value) {
    is String -> listOf(value)
    is Boolean -> listOf(value.toString())
    is Number -> listOf(value.toString())
    is List<*> -> value.map { element ->
      when (element) {
        is String, is Boolean, is Number -> element.toString()
        else -> throw OfrepInvalidContextException(
          "Context attribute '$name' contains a non-primitive element; " +
            "FeatureHub attributes must be strings, numbers, or booleans."
        )
      }
    }
    else -> throw OfrepInvalidContextException(
      "Context attribute '$name' is an object or contains nested objects; " +
        "FeatureHub attributes must be strings, numbers, booleans, or arrays of those."
    )
  }

  /**
   * Maps an edge [FeatureState] (already evaluated for the context by the
   * [io.featurehub.edge.FeatureTransformer]) into an OFREP evaluation entry:
   * either an [OfrepFlagEvaluation] or an [OfrepEvalFailure].
   *
   * Reason synthesis: the FeatureHub edge does not emit reason strings; a
   * matched rollout strategy id (`v`) maps to TARGETING_MATCH, otherwise the
   * value is STATIC. FeatureHub's `locked` flag is unrelated to OpenFeature's
   * DISABLED concept and is not mapped.
   */
  @JvmStatic
  fun toOfrepFlag(featureState: FeatureState, objectMapper: ObjectMapper): Any {
    val metadata = LinkedHashMap<String, Any>()
    featureState.version?.let { metadata["version"] = it }
    featureState.type?.let { metadata["featureType"] = it.toString() }

    val reason = if (featureState.v != null) OfrepReasons.TARGETING_MATCH else OfrepReasons.STATIC

    val rawValue = featureState.value
    if (rawValue == null) {
      // No value set (strings/numbers/json can be value-less): the provider
      // should use the code default. NON_NULL drops the null `value` field,
      // producing OFREP's codeDefaultFlag shape.
      return OfrepFlagEvaluation(featureState.key, null, reason, featureState.v, metadata)
    }

    return when (featureState.type) {
      FeatureValueType.JSON -> {
        // FeatureHub stores JSON feature values as raw strings.
        val parsed: JsonNode? = try {
          objectMapper.readTree(rawValue.toString())
        } catch (e: Exception) {
          null
        }

        if (parsed == null || parsed.isNull || parsed.isMissingNode) {
          OfrepEvalFailure(
            featureState.key,
            OfrepErrorCodes.PARSE_ERROR,
            "Flag '${featureState.key}' contains invalid JSON",
          )
        } else {
          OfrepFlagEvaluation(featureState.key, parsed, reason, featureState.v, metadata)
        }
      }

      else -> OfrepFlagEvaluation(featureState.key, rawValue, reason, featureState.v, metadata)
    }
  }

  /**
   * The bulk-evaluation ETag: the Dacha environment etag (which changes when
   * the flag set changes) combined with the context hash (values differ per
   * context). Format mirrors the edge's existing `<envEtag>//<contextHash>`
   * convention, quoted as a strong ETag.
   */
  @JvmStatic
  fun bulkEtag(environmentEtag: String?, clientContext: ClientContext): String {
    return "\"${environmentEtag ?: "0"}//${clientContext.makeEtag()}\""
  }
}
