package io.featurehub.edge.resources

import io.featurehub.edge.KeyParts
import io.featurehub.edge.features.ETagSplitter.Companion.splitTag
import io.featurehub.edge.strategies.ClientContext
import io.featurehub.sse.model.FeatureStateUpdate
import jakarta.ws.rs.*
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.media.sse.SseFeature
import org.glassfish.jersey.server.ManagedAsync
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

@Path("/features")
@Immediate
class EdgeResource {
  @GET
  @Path("/")
  @Produces("application/json")
  @ManagedAsync
  fun getFeatureStates(
    @Suspended response: AsyncResponse, @QueryParam("sdkUrl") sdkUrls: List<String?>?,
    @QueryParam("apiKey") apiKeys: List<String?>?,
    @HeaderParam("x-featurehub") featureHubAttrs: List<String?>?,
    @HeaderParam("if-none-match") etagHeader: String?
  ) {
    if ((sdkUrls == null || sdkUrls.isEmpty()) && (apiKeys == null || apiKeys.isEmpty())) {
      response.resume(BadRequestException())
      return
    }

    val realApiKeys = (if (sdkUrls == null || sdkUrls.isEmpty()) apiKeys else sdkUrls)!!
        .asSequence()
        .filterNotNull()
      .distinct() // we want unique ones
      .map { KeyParts.fromString(it) }
      .filterNotNull()
      .toList()

    if (realApiKeys.isEmpty()) {
      response.resume(NotFoundException())
      return;
    }

    val clientContext = ClientContext.decode(featureHubAttrs, realApiKeys)
    val etags = splitTag(etagHeader, realApiKeys, clientContext.makeEtag())

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
    @Suspended response: AsyncResponse?, @PathParam("namedCache") namedCache: String?,
    @PathParam("environmentId") envId: UUID?,
    @PathParam("apiKey") apiKey: String?,
    @PathParam("featureKey") featureKey: String?,
    featureStateUpdate: FeatureStateUpdate?
  ) {

  }
}
