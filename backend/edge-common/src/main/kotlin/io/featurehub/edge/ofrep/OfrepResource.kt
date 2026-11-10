package io.featurehub.edge.ofrep

import io.featurehub.jersey.config.JacksonObjectProvider
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * OFREP (OpenFeature Remote Evaluation Protocol) v0.4.0 resource.
 *
 * Implements the OFREP "Core" API set:
 *  - `POST /ofrep/v1/evaluate/flags/{key}`  (server-side, per-call context)
 *  - `POST /ofrep/v1/evaluate/flags`        (bulk, static context, ETag/304)
 *  - `GET  /ofrep/v1/configuration`         (provider metadata + capabilities)
 *
 * Authentication: `Authorization: Bearer <apiKey>` or `X-API-Key: <apiKey>`
 * where `<apiKey>` is a FeatureHub **server-evaluation** SDK key
 * (`[<named-cache>/]<environment-uuid>/<service-key>`).
 *
 * This resource is registered for every edge deployment (edge-rest, edge-full,
 * party-server, party-server-ish) via [io.featurehub.edge.EdgeCommonFeature].
 */
@Path("/ofrep/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Singleton
class OfrepResource
@Inject
constructor(private val evaluationService: OfrepEvaluationService) {
  private val log: Logger = LoggerFactory.getLogger(OfrepResource::class.java)

  @POST
  @Path("evaluate/flags/{key}")
  fun evaluateFlag(
    @PathParam("key") key: String,
    @HeaderParam("Authorization") authorization: String?,
    @HeaderParam("X-API-Key") xApiKey: String?,
    request: OfrepEvaluateRequest?,
  ): Response {
    val token = OfrepEvaluationService.extractToken(authorization, xApiKey)
      ?: return unauthorized()

    return try {
      val apiKey = evaluationService.parseApiKey(token)
      Response.ok(evaluationService.evaluateOne(apiKey, key, request?.context)).build()
    } catch (e: OfrepClientEvalKeyException) {
      forbidden(e)
    } catch (e: OfrepUnauthorizedException) {
      unauthorized()
    } catch (e: OfrepFlagNotFoundException) {
      Response.status(404)
        .entity(OfrepFlagNotFound(key = key, errorDetails = e.message))
        .build()
    } catch (e: OfrepInvalidContextException) {
      Response.status(400)
        .entity(
          OfrepBadEvaluationRequest(
            key = key,
            errorCode = OfrepErrorCodes.INVALID_CONTEXT,
            errorDetails = e.message,
          )
        )
        .build()
    } catch (e: Exception) {
      log.error("OFREP single-flag evaluation of '{}' failed", key, e)
      internalError()
    }
  }

  @POST
  @Path("evaluate/flags")
  fun evaluateFlags(
    @HeaderParam("Authorization") authorization: String?,
    @HeaderParam("X-API-Key") xApiKey: String?,
    @HeaderParam("If-None-Match") ifNoneMatch: String?,
    request: OfrepEvaluateRequest?,
  ): Response {
    val token = OfrepEvaluationService.extractToken(authorization, xApiKey)
      ?: return unauthorized()

    return try {
      val apiKey = evaluationService.parseApiKey(token)
      val (flags, etag) = evaluationService.evaluateAll(apiKey, request?.context)

      if (ifNoneMatch?.trim()?.trim('"') == etag.trim('"')) {
        Response.status(304).header("ETag", etag).build()
      } else {
        Response.ok(OfrepBulkResponse(flags)).header("ETag", etag).build()
      }
    } catch (e: OfrepClientEvalKeyException) {
      forbidden(e)
    } catch (e: OfrepUnauthorizedException) {
      unauthorized()
    } catch (e: OfrepInvalidContextException) {
      Response.status(400)
        .entity(
          OfrepBadEvaluationRequest(
            errorCode = OfrepErrorCodes.INVALID_CONTEXT,
            errorDetails = e.message,
          )
        )
        .build()
    } catch (e: Exception) {
      log.error("OFREP bulk evaluation failed", e)
      internalError()
    }
  }

  @GET
  @Path("configuration")
  fun configuration(): Response {
    val config =
      OfrepConfiguration(
        metadata =
          mapOf(
            "name" to "featurehub",
            "version" to OFREP_VERSION,
          ),
        capabilities =
          mapOf(
            // FeatureHub streams flag changes over SSE; generic OFREP clients
            // should keep polling until dedicated OFREP SSE support exists.
            "cacheInvalidation" to mapOf("type" to "sse", "signal" to "refetchEvaluation"),
          ),
      )
    return Response.ok(config).build()
  }

  private fun unauthorized(): Response = Response.status(Response.Status.UNAUTHORIZED).build()

  private fun forbidden(e: Exception): Response {
    log.warn("OFREP request with client-evaluated API key: {}", e.message)
    return Response.status(Response.Status.FORBIDDEN).build()
  }

  private fun internalError(): Response =
    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
      .entity(OfrepGeneralError("An internal server error occurred while processing the request"))
      .build()

  companion object {
    const val OFREP_VERSION = "0.4.0"
  }
}
