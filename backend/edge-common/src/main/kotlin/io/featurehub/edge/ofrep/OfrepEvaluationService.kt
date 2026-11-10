package io.featurehub.edge.ofrep

import io.featurehub.dacha.api.DachaClientServiceRegistry
import io.featurehub.jersey.config.JacksonObjectProvider
import io.featurehub.dacha.api.DachaApiKeyService
import io.featurehub.edge.FeatureTransformer
import io.featurehub.edge.KeyParts
import io.featurehub.sse.model.FeatureState
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.NotFoundException

/** Thrown when the OFREP request carries no valid SDK API key. */
class OfrepUnauthorizedException :
  Exception("A valid FeatureHub server-evaluation API key is required")

/** Thrown when the key is valid but is a client-evaluated key. */
class OfrepClientEvalKeyException :
  Exception("OFREP requires a server-evaluation API key (client-evaluated keys cannot be used)")

/**
 * Performs OFREP evaluations by delegating to the same Dacha + feature
 * transformation pipeline the edge's `/features` endpoints use.
 */
@Singleton
class OfrepEvaluationService
@Inject
constructor(
  private val dachaRegistry: DachaClientServiceRegistry,
  private val featureTransformer: FeatureTransformer,
) {
  // The edge's single shared Jackson mapper (Kotlin module, NON_NULL).
  private val objectMapper = JacksonObjectProvider.mapper

  /**
   * Parses an OFREP credential into a FeatureHub [KeyParts]. Accepts both
   * `Authorization: Bearer <apiKey>` and `X-API-Key: <apiKey>` upstream; the
   * key format is `[<named-cache>/]<environment-uuid>/<service-key>` (URL-safe
   * base64 tolerated, as the edge's own endpoints do).
   *
   * @throws OfrepUnauthorizedException when the credential is missing/malformed
   *   or resolves to no known environment/service account.
   * @throws OfrepClientEvalKeyException when the key is client-evaluated.
   */
  fun parseApiKey(token: String?): KeyParts {
    if (token.isNullOrBlank()) throw OfrepUnauthorizedException()

    val keyParts = (KeyParts.fromString(token) ?: KeyParts.fromString(token.replace("+", "/")))
      ?: throw OfrepUnauthorizedException()

    if (keyParts.serviceKey.contains("*")) throw OfrepClientEvalKeyException()

    return keyParts
  }

  private fun apiKeyService(key: KeyParts): DachaApiKeyService {
    return try {
      dachaRegistry.getApiKeyService(key.cacheName)
    } catch (e: Exception) {
      throw OfrepUnauthorizedException()
    }
  }

  private fun keyDetails(key: KeyParts) =
    try {
      apiKeyService(key).getApiKeyDetails(key.environmentId, key.serviceKey, true)
    } catch (e: NotFoundException) {
      // Unknown environment or service account is an authentication failure.
      throw OfrepUnauthorizedException()
    }

  /**
   * Evaluates every feature in the environment for the supplied context.
   * Each feature is transformed individually so one bad evaluation cannot
   * silently empty the whole response (the batch transform swallows
   * exceptions into an empty list).
   *
   * @return the OFREP `flags` array and the bulk ETag (quoted).
   */
  fun evaluateAll(key: KeyParts, context: Map<String, Any?>?): Pair<List<Any>, String> {
    val details = keyDetails(key)
    val clientContext = OfrepMapper.toClientContext(context)
    val allowExtendedProperties = details.extendedDataAllowed == true

    val flags: List<Any> =
      (details.features ?: emptyList()).map { cacheFeature ->
        try {
          val featureState: FeatureState =
            featureTransformer.transform(cacheFeature, clientContext, allowExtendedProperties)
          OfrepMapper.toOfrepFlag(featureState, objectMapper)
        } catch (e: Exception) {
          val failedKey = try {
            cacheFeature.feature.key
          } catch (ignored: Exception) {
            null
          }
          OfrepEvalFailure(
            failedKey ?: "unknown",
            OfrepErrorCodes.GENERAL,
            "Feature evaluation failed",
          )
        }
      }

    return Pair(flags, OfrepMapper.bulkEtag(details.etag, clientContext))
  }

  /**
   * Evaluates a single named feature for the supplied context.
   *
   * @throws OfrepFlagNotFoundException when the flag key is unknown.
   */
  fun evaluateOne(key: KeyParts, flagKey: String, context: Map<String, Any?>?): Any {
    val details = keyDetails(key)
    val clientContext = OfrepMapper.toClientContext(context)
    val allowExtendedProperties = details.extendedDataAllowed == true

    val cacheFeature =
      (details.features ?: emptyList()).firstOrNull { it.feature.key == flagKey }
        ?: throw OfrepFlagNotFoundException(flagKey)

    val featureState: FeatureState =
      featureTransformer.transform(cacheFeature, clientContext, allowExtendedProperties)

    return OfrepMapper.toOfrepFlag(featureState, objectMapper)
  }

  companion object {
    /**
     * Extracts the raw API-key token from OFREP authentication headers.
     * Supports `Authorization: Bearer <token>` (case-insensitive scheme) and
     * `X-API-Key: <token>`. Returns null when neither is present.
     */
    @JvmStatic
    fun extractToken(authorization: String?, xApiKey: String?): String? {
      if (!authorization.isNullOrBlank()) {
        val trimmed = authorization.trim()
        val spaceIndex = trimmed.indexOf(' ')
        if (spaceIndex > 0 && trimmed.substring(0, spaceIndex).equals("Bearer", ignoreCase = true)) {
          return trimmed.substring(spaceIndex + 1).trim().ifEmpty { null }
        }
        // A non-Bearer Authorization header falls through to X-API-Key.
      }
      return xApiKey?.trim()?.ifEmpty { null }
    }
  }
}
