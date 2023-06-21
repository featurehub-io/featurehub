package io.featurehub.edge.resources

import cd.connect.jersey.prometheus.Prometheus
import io.featurehub.edge.rest.FeatureGet
import io.featurehub.edge.rest.FeatureUpdate
import io.featurehub.sse.model.FeatureStateUpdate
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.media.sse.SseFeature
import org.glassfish.jersey.server.ManagedAsync
import java.util.*

@Path("/features")
@Immediate
class EdgeResource @Inject constructor(private val featureGetProcessor: FeatureGet, private val featureUpdateProcessor: FeatureUpdate) {
  @GET
  @Path("/")
  @Produces("application/json")
  @Prometheus(name = "edge_poll_api", help = "Number of requests for the poll API")
  @ManagedAsync
  fun getFeatureStates(
    @Suspended response: AsyncResponse, @QueryParam("sdkUrl") sdkUrls: List<String>?,
    @QueryParam("apiKey") apiKeys: List<String>?,
    @HeaderParam("x-featurehub") featureHubAttrs: List<String>?,
    @HeaderParam("if-none-match") etagHeader: String?
  ) {
    featureGetProcessor.processGet(response, sdkUrls, apiKeys, featureHubAttrs, etagHeader, null)
  }

  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Prometheus(name = "edge_sse_api", help = "Number of requests for the SSE API")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  fun features(
    @PathParam("namedCache") namedCache: String?,
    @PathParam("environmentId") envId: UUID?,
    @PathParam("apiKey") apiKey: String?,
    @HeaderParam("x-featurehub") featureHubAttrs: List<String?>?,  // non browsers can set headers
    @QueryParam("xfeaturehub") browserHubAttrs: String?,  // browsers can't set headers,
    @HeaderParam("Last-Event-ID") etag: String?
  ): EventOutput? {
    throw WebApplicationException("SSE not supported in GET-Edge", 501)
  }

  @PUT
  @Path("{namedCache}/{environmentId}/{apiKey}/{featureKey}")
  @ManagedAsync
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  fun update(
    @Suspended response: AsyncResponse, @PathParam("namedCache") namedCache: String,
    @PathParam("environmentId") envId: UUID,
    @PathParam("apiKey") apiKey: String,
    @PathParam("featureKey") featureKey: String,
    featureStateUpdate: FeatureStateUpdate
  ) {
    featureUpdateProcessor.updateFeature(response, namedCache, envId, apiKey, featureKey, featureStateUpdate, null)
  }

  @PUT
  @Path("{environmentId}/{apiKey}/{featureKey}")
  @ManagedAsync
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  fun updateNew(
    @Suspended response: AsyncResponse,
    @PathParam("environmentId") envId: UUID,
    @PathParam("apiKey") apiKey: String,
    @PathParam("featureKey") featureKey: String,
    featureStateUpdate: FeatureStateUpdate
  ) {
    featureUpdateProcessor.updateFeature(response, null, envId, apiKey, featureKey, featureStateUpdate, null)
  }

  @PUT
  @Path("{sdkUrl}/{featureKey}")
  @ManagedAsync
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  fun updateEncoded(
    @Suspended response: AsyncResponse,
    @PathParam("sdkUrl") sdkKey: String,
    @PathParam("featureKey") featureKey: String,
    featureStateUpdate: FeatureStateUpdate
  ) {
    val parts = sdkKey.replace("+", "/").split("/")
    if (parts.size != 2) {
      throw BadRequestException()
    }
    val envId = UUID.fromString(parts[0])
    featureUpdateProcessor.updateFeature(response, null, envId, parts[1], featureKey, featureStateUpdate, null)
  }


}
