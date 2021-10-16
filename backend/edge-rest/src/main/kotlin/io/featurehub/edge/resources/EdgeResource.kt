package io.featurehub.edge.resources

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
  fun update(
    @Suspended response: AsyncResponse, @PathParam("namedCache") namedCache: String,
    @PathParam("environmentId") envId: UUID,
    @PathParam("apiKey") apiKey: String,
    @PathParam("featureKey") featureKey: String,
    featureStateUpdate: FeatureStateUpdate
  ) {
    featureUpdateProcessor.updateFeature(response, namedCache, envId, apiKey, featureKey, featureStateUpdate, null)
  }
}
