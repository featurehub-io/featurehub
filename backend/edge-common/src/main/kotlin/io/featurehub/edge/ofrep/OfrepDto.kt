package io.featurehub.edge.ofrep

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * OFREP (OpenFeature Remote Evaluation Protocol) v0.4.0 wire models.
 *
 * These are hand-written DTOs following the precedent of `InfoResource` /
 * `RestInfoVersion` in common-web: edge resources do not implement the
 * generated `io.featurehub.sse.api` interfaces. The shared Jersey Jackson
 * mapper is configured with NON_NULL, so nullable fields are omitted from
 * responses, exactly as the OFREP spec permits.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
data class OfrepEvaluateRequest(
  /** OpenFeature evaluation context: `targetingKey` plus arbitrary attributes. */
  val context: Map<String, Any?>? = null,
)

/**
 * A successful flag evaluation. `value` is null for JSON-typed flags that have
 * no value set, which serializes (under NON_NULL) to OFREP's `codeDefaultFlag`
 * shape (a success entry with no value).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OfrepFlagEvaluation(
  val key: String,
  val value: Any?,
  /** STATIC | TARGETING_MATCH | SPLIT | DISABLED | UNKNOWN */
  val reason: String,
  /** The id of the matched rollout strategy, if any. */
  val variant: String?,
  val metadata: Map<String, Any>? = null,
)

/** A per-flag failure inside a bulk response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OfrepEvalFailure(
  val key: String,
  /** PARSE_ERROR | TARGETING_KEY_MISSING | INVALID_CONTEXT | GENERAL */
  val errorCode: String,
  val errorDetails: String? = null,
)

/** Bulk evaluation response body. */
data class OfrepBulkResponse(val flags: List<Any>)

/** Single-flag not-found body (HTTP 404). */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OfrepFlagNotFound(
  val key: String,
  val errorCode: String = "FLAG_NOT_FOUND",
  val errorDetails: String? = null,
)

/** Single-flag bad-request body (HTTP 400). */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OfrepBadEvaluationRequest(
  val key: String? = null,
  /** PARSE_ERROR | TARGETING_KEY_MISSING | INVALID_CONTEXT | GENERAL */
  val errorCode: String,
  val errorDetails: String? = null,
)

/** Unexpected server error body (HTTP 500). */
data class OfrepGeneralError(val errorDetails: String)

/** GET /ofrep/v1/configuration — provider metadata and capabilities. */
data class OfrepConfiguration(
  val metadata: Map<String, String>,
  val capabilities: Map<String, Any>,
)
