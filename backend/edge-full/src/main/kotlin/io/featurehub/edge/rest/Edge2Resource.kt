package io.featurehub.edge.rest

import cd.connect.jersey.prometheus.Prometheus
import io.featurehub.sse.model.FeatureStateUpdate
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.media.sse.SseFeature
import org.glassfish.jersey.server.ManagedAsync
import java.util.*

@Path("/f2")
class Edge2Resource @Inject constructor(
  private val sseProcessor: EdgeServerSentEventsProcessor,
  private val featureGetProcessor: EdgeGet,
  private val featureUpdateProcessor: FeatureUpdate
) {
  // support new and old style for GET - apiKeys and sdkUrl, they are the same we just want to
  // transition the
  // naming at some point
  @GET
  @Path("/g")
  @Produces("application/json")
  @Prometheus(name = "edge_poll_api", help = "Number of requests for the poll API")
  @ManagedAsync
  fun getFeatureStates(
    @Suspended response: AsyncResponse,
    @QueryParam("apiKey") apiKeys: List<String>?,
    @Context uriInfo: UriInfo,
    @HeaderParam("if-none-match") etagHeader: String?
  ) {
    if (apiKeys.isNullOrEmpty()) {
      response.resume(
        Response.status(400).header("content-type", "text/plain").entity(
          "No API keys provided "
        ).build()
      )
      return
    }
    featureGetProcessor.process(
      response, apiKeys, etagHeader, uriInfo.queryParameters
    )
  }

  @GET
  @Path("/s")
  @Prometheus(name = "edge_sse_api", help = "Number of requests for the SSE API")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  fun features(
    @QueryParam("apiKey") apiKeys: List<String>?,
    @Context uriInfo: UriInfo,
    @HeaderParam("x-fh-extraconfig") extraConfig: String?,
    @HeaderParam("Last-Event-ID") etag: String?
  ): EventOutput {
    return sseProcessor.process(apiKeys, uriInfo.queryParameters, etag, extraConfig)
  }

  @PUT
  @Path("{environmentId}/{apiKey}/{featureKey}")
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  @ManagedAsync
  fun update(
    @Suspended response: AsyncResponse?,
    @PathParam("environmentId") envId: UUID?,
    @PathParam("apiKey") apiKey: String?,
    @PathParam("featureKey") featureKey: String?,
    featureStateUpdate: FeatureStateUpdate?
  ) {
    featureUpdateProcessor.updateFeature(
      response!!, null, envId!!, apiKey!!, featureKey!!, featureStateUpdate!!, null
    )
  }

  @PUT
  @Path("{apiKey}/{featureKey}")
  @ManagedAsync
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  fun updateEncoded(
    @Suspended response: AsyncResponse,
    @PathParam("apiKey") apiKey: String,
    @PathParam("featureKey") featureKey: String,
    featureStateUpdate: FeatureStateUpdate
  ) {
    val parts = apiKey.replace("+", "/").split("/")
    if (parts.size != 2) {
      throw BadRequestException()
    }
    val envId = UUID.fromString(parts[0])
    featureUpdateProcessor.updateFeature(response, null, envId, parts[1], featureKey, featureStateUpdate, null)
  }
}
