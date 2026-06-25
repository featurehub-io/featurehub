package io.featurehub.edge.rest

import cd.connect.jersey.prometheus.Prometheus
import io.featurehub.edge.stats.StatRecorder
import io.featurehub.edge.utils.FastlyResponseWrapper
import io.featurehub.sse.model.FeatureStateUpdate
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.Response
import org.glassfish.hk2.api.Immediate
import org.glassfish.jersey.media.sse.EventOutput
import org.glassfish.jersey.media.sse.SseFeature
import org.glassfish.jersey.server.ManagedAsync
import java.util.*

@Path("/features")
@Immediate
class EventStreamResource @Inject constructor(
  private val statRecorder: StatRecorder,
  private val featureSse: FeatureSse,
  private val featureGetProcessor: FeatureGet,
  private val featureUpdateProcessor: FeatureUpdate
) {
  private val fastlyConfigured: Boolean

  init {
    fastlyConfigured = FastlyResponseWrapper.fastlyConfigured()
  }

  // support new and old style for GET - apiKeys and sdkUrl, they are the same we just want to
  // transition the
  // naming at some point
  @GET
  @Path("/")
  @Produces("application/json")
  @Prometheus(name = "edge_poll_api", help = "Number of requests for the poll API")
  @ManagedAsync
  fun getFeatureStates(
    @Suspended response: AsyncResponse,
    @QueryParam("sdkUrl") sdkUrls: List<String>?,
    @QueryParam("apiKey") apiKeys: List<String>?,
    @QueryParam("contextSha") contextSha: String?,
    @HeaderParam("x-featurehub") featureHubAttrs: List<String>?,
    @HeaderParam("if-none-match") etagHeader: String?
  ) {
    if (fastlyConfigured && contextSha == null) {
      response.resume(
        Response.status(400).header("content-type", "text/plain").entity(
          "Fastly is configured and your SDK is too old to support it and will " +
            "malfunction, please update."
        ).build()
      )
      return
    }
    featureGetProcessor.processGet(
      response!!, sdkUrls, apiKeys, featureHubAttrs, etagHeader
    )
  }

  @GET
  @Path("{namedCache}/{environmentId}/{apiKey}")
  @Prometheus(name = "edge_sse_api", help = "Number of requests for the SSE API")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  fun features(
    @PathParam("namedCache") namedCache: String?,
    @PathParam("environmentId") envId: UUID?,
    @PathParam("apiKey") apiKey: String?,
    @HeaderParam("x-featurehub") featureHubAttrs: List<String>?,  // non browsers can set headers
    @HeaderParam("x-fh-extraconfig") extraConfig: String?,
    @QueryParam("xfeaturehub") browserHubAttrs: String?,  // browsers can't set headers,
    @HeaderParam("Last-Event-ID") etag: String?
  ): EventOutput {
    return featureSse.process(namedCache, envId!!, apiKey!!, featureHubAttrs, browserHubAttrs, etag, extraConfig)
  }

  @GET
  @Path("{environmentId}/{apiKey}")
  @Prometheus(name = "edge_sse_api", help = "Number of requests for the SSE API")
  @Produces(SseFeature.SERVER_SENT_EVENTS)
  fun features(
    @PathParam("environmentId") envId: UUID?,
    @PathParam("apiKey") apiKey: String?,
    @HeaderParam("x-featurehub") featureHubAttrs: List<String>?,  // non browsers can set headers
    @HeaderParam("x-fh-extraconfig") extraConfig: String?,
    @QueryParam("xfeaturehub") browserHubAttrs: String?,  // browsers can't set headers,
    @HeaderParam("Last-Event-ID") etag: String?
  ): EventOutput {
    return featureSse.process(null, envId!!, apiKey!!, featureHubAttrs, browserHubAttrs, etag, extraConfig)
  }

  /**
   * We do a double check of all permissions and values at Edge to ensure that as much load as
   * possible is kept off MR. MR will do these checks against this set of permissions again, but
   * updates are done via NATs and not via REST.
   */
  @PUT
  @Path("{namedCache}/{environmentId}/{apiKey}/{featureKey}")
  @Prometheus(name = "edge_test_sdk_api", help = "Number of requests to the test SDK API")
  @ManagedAsync
  fun update(
    @Suspended response: AsyncResponse?,
    @PathParam("namedCache") namedCache: String?,
    @PathParam("environmentId") envId: UUID?,
    @PathParam("apiKey") apiKey: String?,
    @PathParam("featureKey") featureKey: String?,
    featureStateUpdate: FeatureStateUpdate?
  ) {
    featureUpdateProcessor.updateFeature(
      response!!, namedCache, envId!!, apiKey!!, featureKey!!, featureStateUpdate!!, statRecorder
    )
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
      response!!, null, envId!!, apiKey!!, featureKey!!, featureStateUpdate!!, statRecorder
    )
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
      response.resume(
        Response.status(400).header("content-type", "text/plain").entity(
          "Invalid key"
        ).build()
      )
      return
    }
    val envId = UUID.fromString(parts[0])
    featureUpdateProcessor.updateFeature(response, null, envId, parts[1], featureKey, featureStateUpdate, null)
  }

}
